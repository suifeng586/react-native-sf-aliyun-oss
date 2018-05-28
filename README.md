# react-native-sf-aliyun-oss


# 阿里云文件上传、下载


# 安装
* npm install react-native-sf-aliyun-oss
* react-native link react-native-sf-aliyun-oss


# Methods
|  Methods  |  Params  |  Param Types  |   description  |  Example  |
|:-----|:-----|:-----|:-----|:-----|
|config|accessKey, secretKey, endPoint, bucketName|string,string,string,string|阿里云配置参数|参考例子|
|upload|folder,filePath,progress,suc,fail|string/string/func/func/func|上传文件|参考例子|
|uploadCompress|folder,filePath,progress,suc,fail|string/string/func/func/func|上传文件,带压缩|参考例子|
|uploadCompressExt|folder,filePath,fileExt,progress,suc,fail|string/string/string/func/func/func|上传文件，带压缩，链接不带文件后缀的需要调用此函数|参考例子|
|uploadMul|folder,filePaths,progress,suc,fail|string/array/func/func/func|上传多个文件|参考例子|
|uploadMulCompress|folder,filePaths,progress,suc,fail|string/array/func/func/func|上传多个文件，带压缩|参考例子|
|uploadMulCompressExt|folder,filePaths,fileExts,progress,suc,fail|string/array,array/func/func/func|上传多个文件，带压缩，链接不带文件后缀的需要调用此函数|参考例子|
|downLoad|tag,filePath,expireTime,progress,suc,fail|string/string/int/func/func/func|下载文件|参考例子|


# 例子
```
import SFAliyunOss from 'react-native-sf-aliyun-oss';

//配置一次
SFAliyunOss.config('阿里云的AccessKey','阿里云的SecretKey','阿里云的endPoint','要上传到的bucketName')

//文件上传
SFAliyunOss.upload('阿里云文件目录','上传文件的地址',(progress)=>{
              console.log(progress);
            },(fileKey)=>{
                console.log(fileKey);
            },(err)=>{
                console.log(err);
            });

//文件上传,带压缩（只支持图片和视频）
SFAliyunOss.upload('阿里云文件目录','上传文件的地址',(progress)=>{
              console.log(progress);
            },(fileKey)=>{
                console.log(fileKey);
            },(err)=>{
                console.log(err);
            });

//多文件上传
SFAliyunOss.uploadMul('阿里云文件目录',array('上传文件的所有地址列表'),(progress)=>{
                console.log(progress);
            },(fileKeys)=>{
                console.log(fileKeys);
            },(err)=>{
                console.log(err);
            });

//多文件上传,带压缩（只支持图片和视频）
SFAliyunOss.uploadMulCompress('阿里云文件目录',array('上传文件的所有地址列表'),(progress)=>{
                console.log(progress);
            },(fileKeys)=>{
                console.log(fileKeys);
            },(err)=>{
                console.log(err);
            });

//文件下载,expireTime：下载文件缓存过期时间，单位分钟
SFAliyunOss.download('10','oss文件key',30,(progress)=>{
                console.log(progress);
            },(filePath)=>{
                console.log(filePath);
            },(err)=>{

            });

```


