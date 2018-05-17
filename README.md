# react-native-sf-aliyun-oss


# 阿里云文件上传、下载


# 安装
* npm install react-native-sf-aliyun-oss
* npm install react-native-aliyun-oss-cp
* react-native link react-native-aliyun-oss-cp


# Methods
|  Methods  |  Params  |  Param Types  |   description  |  Example  |
|:-----|:-----|:-----|:-----|:-----|
|config|config|object|阿里云配置参数|参考例子|
|upload|folder/filePath/ext/progress/suc/fail|string/string/string/func/func/func|上传文件|参考例子|
|downLoad|filePath/progress/suc/fail|string/func/func/func|下载文件|参考例子|


# 例子
```
import SFAliyunOss from 'react-native-sf-aliyun-oss';

//配置一次
const config = {
    AccessKey: '阿里云的AccessKey',
    SecretKey: '阿里云的SecretKey',
    SecretToken: '阿里云的SecretToken',
    endPoint: '阿里云的endPoint',
    bucketName: '阿里云的bucketName'
  }
SFAliyunOss.config(config)
//文件上传
SFAliyunOss.upload('阿里云文件目录','上传文件的地址','文件后缀名',(progress)=>{},(fileKey)=>{},(err)=>{})
//多文件上传
SFAliyunOss.uploadMulti('阿里云文件目录',[{filePath:'',ext:'png'},{filePath:'',ext:'png'}],(progress)=>{},(fileKeys)=>{},(err)=>{})
//文件下载
SFAliyunOss.downLoad('阿里云文件地址',(progress)=>{},(filePath)=>{},(err)=>{})

```

