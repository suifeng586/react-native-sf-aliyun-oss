/**
 * Created by SF on 2018/4/8.
 */

import React,{Component} from 'react';
import AliyunOSS from 'react-native-aliyun-oss-cp';

export default class SFAliyunOss extends Component{

    static info = null
    static multiKeys = []

    /***
     * oss配置 只需配置一次
     * @param config
     */
    static config(config){
        SFAliyunOss.info = config
        var data = {
            AccessKey: config.AccessKey,
            SecretKey: config.SecretKey,
            SecretToken: config.SecretToken,
        }
        AliyunOSS.initWithKey(data,config.endPoint);
    }

    /***
     * 文件上传
     * @param folder 文件在oss上的目录
     * @param filePath 文件本地路径
     * @param ext 文件后缀名 （.png .mp4）
     * @param progress 上传进度回调
     * @param suc 上传成功回调
     * @param fail 上传失败回调
     */
    static upload(folder,filePath,ext,progress, suc, fail) {
        //路径的转换 去掉  file:// 阿里云不会处理
        filePath = filePath.replace('file://', '')
        //上传文件的名字  自己随便起 但不要重复 最好有时间戳
        var fileName = this.getFileName(ext)

        // upload config
        const uploadConfig = {
            bucketName: SFAliyunOss.info.bucketName,  //your bucketName
            sourceFile: filePath, // local file path
            ossFile: folder + '/' + fileName // the file path uploaded to oss
        }

        // 上传进度
        const uploadProgress = p => progress(p.currentSize / p.totalSize)

        // 增加上传事件监听
        AliyunOSS.addEventListener('uploadProgress', uploadProgress)

        // 执行上传
        AliyunOSS.uploadObjectAsync(uploadConfig).then((success) => {
            // 去除事件监听
            AliyunOSS.removeEventListener('uploadProgress', uploadProgress)
            suc(fileName)
        }).catch((err) => {
            AliyunOSS.removeEventListener('uploadProgress', uploadProgress)
            fail(err)
        })

    }

    /***
     * 多个文件上传
     * @param folder
     * @param filePaths [{filePath:'',ext:'png'},{filePath:'',ext:'png'}]
     * @param progress
     * @param suc
     * @param fail
     */
    static uploadMulti(folder,filePaths,progress,suc,fail){
        const count = filePaths.length;
        this.uploadSingle(0,count,folder,filePaths,progress,suc,fail);
    }
    static uploadSingle(index,count,folder,filePaths,progress,suc,fail) {
        if (index >= count){
            if (suc){
                suc(SFAliyunOss.multiKeys)
            }
            return;
        }
        var data = filePaths[index];
        var p_start = index/count;
        this.upload(folder, data.filePath, data.ext, (value)=>{
            if (progress){
                progress(p_start+value/count)
            }
        }, (key)=>{
            SFAliyunOss.multiKeys.push(key);
            this.uploadSingle(index+1,folder,count,filePaths,progress,suc,fail)
        }, (err)=>{
            if (fail){
                fail(err)
            }
        })
    }
    /***
     * 文件下载
     * @param filePath 文件在oss上的路径
     * @param progress 上传进度回调
     * @param suc 上传成功回调
     * @param fail 上传失败回调
     */
    static downLoad(filePath,progress, suc, fail) {
        const downloadConfig = {
            bucketName: SFAliyunOss.info.bucketName,
            ossFile: filePath // the file path on the oss
        };
        const downloadProgress = p => progress(p.currentSize / p.totalSize);
        AliyunOSS.addEventListener('downloadProgress', downloadProgress);
        AliyunOSS.downloadObjectAsync(downloadConfig).then(path => {
            suc(path); // the local file path downloaded from oss
            AliyunOSS.removeEventListener('downloadProgress', downloadProgress);
        }).catch((error) => {
            fail(error);
            AliyunOSS.removeEventListener('downloadProgress', downloadProgress);
        });

    }

    //最终格式 (前半段)2017-11-1_(后半段)132132321312

    //获取文件名  文件名的前半部分
    static getFileName(ext) {
        var timestamp = new Date().getTime()
        var fileName = this.getNowFormatDate() + '_' + timestamp+'.'+ext;
        return fileName;
    }

    static getNowFormatDate() {
        var date = new Date();
        var seperator1 = "-";
        var year = date.getFullYear();
        var month = date.getMonth() + 1;
        var strDate = date.getDate();
        if (month >= 1 && month <= 9) {
            month = "0" + month;
        }
        if (strDate >= 0 && strDate <= 9) {
            strDate = "0" + strDate;
        }
        var currentdate = year + seperator1 + month + seperator1 + strDate;
        return currentdate;
    }

}
