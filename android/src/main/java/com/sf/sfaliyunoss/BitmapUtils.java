package com.sf.sfaliyunoss;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by LC on 2016/6/27.
 */
public class BitmapUtils {
    public static Bitmap ratio(String imgPath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inScaled  = false;
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath,options);
        return bitmap;
    }
    public static byte[] Bitmap2Bytes(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
    }
    public static Bitmap readBitmap(String path, String name){
        File file = new File(path, name);
        if(file.exists()){
            Bitmap bm = BitmapFactory.decodeFile(file.getPath());
            return bm;
        }else{
            return null;
        }
    }
    public static void saveBitmap(String path, String name, Bitmap mBitmap){
        File f = new File(path,name);
        try {
            if(!f.exists()){
                f.createNewFile();
            }
            FileOutputStream fOut = null;
            fOut = new FileOutputStream(f);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (NullPointerException e) {
            // TODO: handle exception
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Bitmap loadBitmap(String imgpath, Bitmap bitmap, boolean adjustOritation) {
        if (!adjustOritation) {
            return bitmap;
        } else {
            Bitmap bm = bitmap;
            int digree = 0;
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(imgpath);
            } catch (IOException e) {
                exif = null;
            }
            if (exif != null) {
                // 读取图片中相机方向信息
                int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);
                // 计算旋转角度
                switch (ori) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        digree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        digree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        digree = 270;
                        break;
                    default:
                        digree = 0;
                        break;
                }
            }
            if (digree != 0) {
                // 旋转图片
                Matrix m = new Matrix();
                m.postRotate(digree);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                        bm.getHeight(), m, true);
            }
            return bm;
        }
    }
    public static void getHttpBitmap(String url, onCallBack callBack) {
        new getHttpBitmapTask().execute(url,callBack);
    }
    static class getHttpBitmapTask extends AsyncTask<Object,Object,Bitmap> {
        onCallBack callBack;
        @Override
        protected Bitmap doInBackground(Object... params) {
            callBack = (onCallBack)params[1];
            Bitmap bitmap = null;
            try {
                URL pictureUrl = new URL(params[0].toString());
                InputStream in = pictureUrl.openStream();
                bitmap = BitmapFactory.decodeStream(in);
                in.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap!=null){
                callBack.onSuccess(bitmap);
            }
        }
    }
    public interface onCallBack{
        void onSuccess(Bitmap b);
    }
}
