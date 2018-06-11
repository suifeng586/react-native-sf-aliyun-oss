package com.sf.sfaliyunoss;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.zero.smallvideorecord.DeviceUtils;
import com.zero.smallvideorecord.JianXiCamera;
import com.zero.smallvideorecord.LocalMediaCompress;
import com.zero.smallvideorecord.model.AutoVBRMode;
import com.zero.smallvideorecord.model.LocalMediaConfig;
import com.zero.smallvideorecord.model.OnlyCompressOverBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Administrator on 2018-05-28.
 */

public class AliyunOSSModule extends ReactContextBaseJavaModule {
    String accessKey = "";
    String secretKey = "";
    String securityToken = "";
    String endpoint = "";
    String bucketName = "";
    OSS oss = null;
    long totalsize = 0;

    public AliyunOSSModule(ReactApplicationContext reactContext) {
        super(reactContext);
        initSmallVideo();
    }

    @Override
    public String getName() {
        return "SFAliyunOssBridge";
    }

    @ReactMethod
    public void initWithKey(String accessKey, String secretKey, String securityToken, String endpoint, String bucketName) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.securityToken = securityToken;
        this.bucketName = bucketName;
        OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(accessKey, secretKey, securityToken);
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求数，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
//        OSSLog.enableLog();
        this.oss = new OSSClient(getReactApplicationContext(), endpoint, credentialProvider);
    }

    @ReactMethod
    public void download(final String tag, String ossFile, final String fileExt, final int expireTime, final Promise promise) {
        GetObjectRequest get = new GetObjectRequest(this.bucketName, ossFile);
        get.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                WritableMap map = new WritableNativeMap();
                map.putString("tag", tag);
                map.putString("totalByte", "" + currentSize);
                map.putString("totalBytesExpected", "" + totalSize);
                getReactApplicationContext()
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("eventDownloadProgress", map);
            }
        });
        OSSAsyncTask task = oss.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
            @Override
            public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                // 请求成功
                InputStream inputStream = result.getObjectContent();
                long time = System.currentTimeMillis() + expireTime * 60 * 1000;
                String dir = getTmpDir();
                File f = new File(dir, "" + time + "." + fileExt);
                byte[] buffer = new byte[2048];
                int len;
                try {
                    OutputStream os = new FileOutputStream(f);
                    while ((len = inputStream.read(buffer)) != -1) {
                        // 处理下载的数据
                        os.write(buffer, 0, len);
                    }
                    os.close();
                    inputStream.close();
                    promise.resolve(dir + "/" + time + "." + fileExt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
//                    Log.e("ErrorCode", serviceException.getErrorCode());
//                    Log.e("RequestId", serviceException.getRequestId());
//                    Log.e("HostId", serviceException.getHostId());
//                    Log.e("RawMessage", serviceException.getRawMessage());
                    promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                }
            }
        });
    }

    @ReactMethod
    public void uploadSingle(String sourceFile, final String ossFile, final Promise promise) {
        try {
            File file = new File(new URI(sourceFile));
            PutObjectRequest put = new PutObjectRequest(bucketName, ossFile, file.getPath());
            put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                @Override
                public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                    WritableMap map = new WritableNativeMap();
                    map.putString("totalByte", "" + currentSize);
                    map.putString("totalBytesExpected", "" + totalSize);
                    map.putString("progress", String.format("%.2f", (float) currentSize / (float) totalSize));
                    getReactApplicationContext()
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("eventUploadProgress", map);
                }
            });
            OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                @Override
                public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                    Log.d("PutObject", "UploadSuccess");
                    Log.d("ETag", result.getETag());
                    Log.d("RequestId", result.getRequestId());
                    promise.resolve(ossFile);
                }

                @Override
                public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                    // 请求异常
                    if (clientExcepion != null) {
                        // 本地异常如网络异常等
                        clientExcepion.printStackTrace();
                    }
                    if (serviceException != null) {
                        // 服务异常
//                    Log.e("ErrorCode", serviceException.getErrorCode());
//                    Log.e("RequestId", serviceException.getRequestId());
//                    Log.e("HostId", serviceException.getHostId());
//                    Log.e("RawMessage", serviceException.getRawMessage());
                        promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                    }
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void uploadMul(ReadableArray sourceFiles, ReadableArray ossFiles, Promise promise) {
        this.upload(0, sourceFiles, ossFiles, new WritableNativeArray(), promise);
    }

    @ReactMethod
    public void uploadSingleCompress(final String sourceFile, int fileType, final String ossFile, final Promise promise) {
        if (fileType == 0) {
            try {
                File file = new File(new URI(sourceFile));
                Bitmap b = BitmapUtils.ratio(file.getPath());
                PutObjectRequest put = new PutObjectRequest(bucketName, ossFile, BitmapUtils.Bitmap2Bytes(b));
                put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                    @Override
                    public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                        WritableMap map = new WritableNativeMap();
                        map.putString("totalByte", "" + currentSize);
                        map.putString("totalBytesExpected", "" + totalSize);
                        map.putString("progress", String.format("%.2f", (float) currentSize / (float) totalSize));
                        getReactApplicationContext()
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("eventUploadProgress", map);
                    }
                });
                OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                    @Override
                    public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                        Log.d("PutObject", "UploadSuccess");
                        Log.d("ETag", result.getETag());
                        Log.d("RequestId", result.getRequestId());
                        promise.resolve(ossFile);
                    }

                    @Override
                    public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                        // 请求异常
                        if (clientExcepion != null) {
                            // 本地异常如网络异常等
                            clientExcepion.printStackTrace();
                        }
                        if (serviceException != null) {
                            // 服务异常
//                    Log.e("ErrorCode", serviceException.getErrorCode());
//                    Log.e("RequestId", serviceException.getRequestId());
//                    Log.e("HostId", serviceException.getHostId());
//                    Log.e("RawMessage", serviceException.getRawMessage());
                            promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                        }
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            try {
                File file = new File(new URI(sourceFile));
                this.startVideoCompress(file.getPath(), new compressCallback() {
                    @Override
                    public void onComplete(String path) {
                        Log.e("sf", path);
                        PutObjectRequest put = new PutObjectRequest(bucketName, ossFile, path);
                        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                            @Override
                            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                                WritableMap map = new WritableNativeMap();
                                map.putString("totalByte", "" + currentSize);
                                map.putString("totalBytesExpected", "" + totalSize);
                                map.putString("progress", String.format("%.2f", (float) currentSize / (float) totalSize));
                                getReactApplicationContext()
                                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                        .emit("eventUploadProgress", map);
                            }
                        });
                        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                            @Override
                            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                                Log.d("PutObject", "UploadSuccess");
                                Log.d("ETag", result.getETag());
                                Log.d("RequestId", result.getRequestId());
                                promise.resolve(ossFile);
                            }

                            @Override
                            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                                // 请求异常
                                if (clientExcepion != null) {
                                    // 本地异常如网络异常等
                                    clientExcepion.printStackTrace();
                                }
                                if (serviceException != null) {
                                    // 服务异常
                                    promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                                }
                            }
                        });
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }

    }

    @ReactMethod
    public void uploadMulCompress(ReadableArray sourceFiles, ReadableArray fileTypes, ReadableArray ossFiles, Promise promise) {
        this.uploadcompress(0, sourceFiles, fileTypes, ossFiles, new WritableNativeArray(), promise);
    }

    @ReactMethod
    public void clearExpireFileCache(Promise promise) {
        delFolder(getTmpDir(),false);
        promise.resolve(0);
    }

    @ReactMethod
    public void clearAllCache(Promise promise) {
        delFolder(getTmpDir(),true);
        promise.resolve(0);
    }

    @ReactMethod
    public void getCacheSizeTmp(Promise promise) {
        totalsize = 0;
        searchCacheTmp(new File(getTmpDir()));
        promise.resolve("" + totalsize);
    }

    @ReactMethod
    public void getCacheSizeAll(Promise promise) {
        totalsize = 0;
        searchCacheAll(new File(getTmpDir()));
        promise.resolve("" + totalsize);
    }

    @ReactMethod
    public void getTotalMemorySize(Promise promise) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        promise.resolve("" + totalBlocks * blockSize);
    }
    @ReactMethod
    public void getCacheSizeByPath(String path,Promise promise){
        File file = new File(path);
        if(file.exists()){
            promise.resolve(""+file.length());
        }else{
            promise.reject("-100","file is not exists");
        }
    }

    private void upload(final int index, final ReadableArray sourceFiles, final ReadableArray ossFiles, final WritableNativeArray resultarray, final Promise promise) {
        final String ossFile = ossFiles.getString(index);
        String sourceFile = sourceFiles.getString(index);
        try {
            File file = new File(new URI(sourceFile));
            PutObjectRequest put = new PutObjectRequest(bucketName, ossFile, file.getPath());
            put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                @Override
                public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                    WritableMap map = new WritableNativeMap();
                    map.putString("totalByte", "" + currentSize);
                    map.putString("totalBytesExpected", "" + totalSize);
                    map.putString("progress", String.format("%.2f", (1 / ossFiles.size()) * ((float) currentSize / (float) totalSize)) + index);
                    getReactApplicationContext()
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                            .emit("eventUploadProgress", map);
                }
            });
            OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                @Override
                public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                    Log.d("PutObject", "UploadSuccess");
                    Log.d("ETag", result.getETag());
                    Log.d("RequestId", result.getRequestId());
                    if (index + 1 < ossFiles.size()) {
                        resultarray.pushString(ossFile);
                        upload(index + 1, sourceFiles, ossFiles, resultarray, promise);
                    } else {
                        resultarray.pushString(ossFile);
                        promise.resolve(resultarray);
                    }
                }

                @Override
                public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                    // 请求异常
                    if (clientExcepion != null) {
                        // 本地异常如网络异常等
                        clientExcepion.printStackTrace();
                    }
                    if (serviceException != null) {
                        // 服务异常
//                    Log.e("ErrorCode", serviceException.getErrorCode());
//                    Log.e("RequestId", serviceException.getRequestId());
//                    Log.e("HostId", serviceException.getHostId());
//                    Log.e("RawMessage", serviceException.getRawMessage());
                        promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                    }
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void uploadcompress(final int index, final ReadableArray sourceFiles, final ReadableArray fileTypes, final ReadableArray ossFiles, final WritableNativeArray resultarray, final Promise promise) {
        final String ossFile = ossFiles.getString(index);
        String sourceFile = sourceFiles.getString(index);
        int fileType = fileTypes.getInt(index);
        if (fileType == 0) {
            try {
                File file = new File(new URI(sourceFile));
                Bitmap b = BitmapUtils.ratio(file.getPath());
                PutObjectRequest put = new PutObjectRequest(bucketName, ossFile, BitmapUtils.Bitmap2Bytes(b));
                put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                    @Override
                    public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                        WritableMap map = new WritableNativeMap();
                        map.putString("totalByte", "" + currentSize);
                        map.putString("totalBytesExpected", "" + totalSize);
                        map.putString("progress", String.format("%.2f", (1 / ossFiles.size()) * ((float) currentSize / (float) totalSize)) + index);
                        getReactApplicationContext()
                                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit("eventUploadProgress", map);
                    }
                });
                OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                    @Override
                    public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                        Log.d("PutObject", "UploadSuccess");
                        Log.d("ETag", result.getETag());
                        Log.d("RequestId", result.getRequestId());
                        if (index + 1 < ossFiles.size()) {
                            resultarray.pushString(ossFile);
                            uploadcompress(index + 1, sourceFiles, fileTypes, ossFiles, resultarray, promise);
                        } else {
                            resultarray.pushString(ossFile);
                            promise.resolve(resultarray);
                        }
                    }

                    @Override
                    public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                        // 请求异常
                        if (clientExcepion != null) {
                            // 本地异常如网络异常等
                            clientExcepion.printStackTrace();
                        }
                        if (serviceException != null) {
                            // 服务异常
//                    Log.e("ErrorCode", serviceException.getErrorCode());
//                    Log.e("RequestId", serviceException.getRequestId());
//                    Log.e("HostId", serviceException.getHostId());
//                    Log.e("RawMessage", serviceException.getRawMessage());
                            promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                        }
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            try {
                File file = new File(new URI(sourceFile));
                this.startVideoCompress(file.getPath(), new compressCallback() {
                    @Override
                    public void onComplete(String path) {
                        Log.e("sf", path);
                        PutObjectRequest put = new PutObjectRequest(bucketName, ossFile, path);
                        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
                            @Override
                            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                                WritableMap map = new WritableNativeMap();
                                map.putString("totalByte", "" + currentSize);
                                map.putString("totalBytesExpected", "" + totalSize);
                                map.putString("progress", String.format("%.2f", (1 / ossFiles.size()) * ((float) currentSize / (float) totalSize)) + index);
                                getReactApplicationContext()
                                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                        .emit("eventUploadProgress", map);
                            }
                        });
                        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                            @Override
                            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                                Log.d("PutObject", "UploadSuccess");
                                Log.d("ETag", result.getETag());
                                Log.d("RequestId", result.getRequestId());
                                if (index + 1 < ossFiles.size()) {
                                    resultarray.pushString(ossFile);
                                    uploadcompress(index + 1, sourceFiles, fileTypes, ossFiles, resultarray, promise);
                                } else {
                                    resultarray.pushString(ossFile);
                                    promise.resolve(resultarray);
                                }
                            }

                            @Override
                            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                                // 请求异常
                                if (clientExcepion != null) {
                                    // 本地异常如网络异常等
                                    clientExcepion.printStackTrace();
                                }
                                if (serviceException != null) {
                                    // 服务异常
                                    promise.reject(serviceException.getErrorCode(), serviceException.getRawMessage());
                                }
                            }
                        });
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }
    }

    private String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    private String getTmpDir() {
        String tmpDir = getDiskCachePath(getReactApplicationContext()) + "/react-native-sf-aliyunoss";
        Boolean created = new File(tmpDir).mkdir();
        return tmpDir;
    }

    private void startVideoCompress(final String path, final compressCallback callback) {
        LocalMediaConfig.Buidler buidler = new LocalMediaConfig.Buidler();
        final LocalMediaConfig config = buidler
                .setVideoPath(path)
                .captureThumbnailsTime(1)
                .doH264Compress(new AutoVBRMode())
                .setFramerate(10)
                .build();
        new Thread(new Runnable() {
            @Override
            public void run() {
                OnlyCompressOverBean onlyCompressOverBean = new LocalMediaCompress(config).startCompress();
                callback.onComplete(onlyCompressOverBean.getVideoPath());
            }
        }).start();
    }

    public void initSmallVideo() {
        JianXiCamera.setVideoCachePath(getDiskCachePath(getReactApplicationContext()) + "/react-native-sf-aliyunoss/");
        JianXiCamera.initialize(false, null);
    }

    private void searchCacheTmp(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    searchCacheTmp(childFile);
                }
            }
        } else {
            try {
                if (Long.parseLong(file.getName().substring(0, file.getName().indexOf("."))) > 0) {
                    totalsize += file.length();
                }
            } catch (Exception e) {

            }
        }
    }

    private void searchCacheAll(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    searchCacheAll(childFile);
                }
            }
        } else {
            try {
                totalsize += file.length();
            } catch (Exception e) {

            }
        }
    }


    public void delFolder(String folderPath,boolean isall) {
        try {
            delAllFile(folderPath,isall); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean delAllFile(String path,boolean isall) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                if(!isall){
                    long time = System.currentTimeMillis();
                    if (time > Long.parseLong(temp.getName().substring(0, temp.getName().indexOf(".")))) {
                        temp.delete();
                    }
                }else{
                    temp.delete();
                }

            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i],isall);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i],isall);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

interface compressCallback {
    void onComplete(String path);
}
}
