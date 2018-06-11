/**
 * Created by SF on 2018/4/8.
 */

import React, {Component} from 'react';
import {
    NativeModules,
    NativeEventEmitter,
    NativeAppEventEmitter,
    Platform
} from 'react-native';

const oss = NativeModules.SFAliyunOssBridge;
const _subscriptions = new Map();
const SFAliyunOss = {


    config(accessKey, secretKey, endPoint, bucketName) {
        oss.initWithKey(accessKey, secretKey, '', endPoint, bucketName);
    },
    setImgSize(maxSize){//单位kb
        if (Platform.OS == 'ios'){
            oss.setImageMaxSize(maxSize);
        }
    },

    deleteFile(filePath,callBack){
        if (Platform.OS == 'ios') {
            oss.deleteFile(filePath,(err,filePath)=>{
                callBack(filePath);
            })
        }else{
            oss.deleteFile(filePath).then((filePath)=> {
                callBack(filePath);
            }).catch((e) => {

            });
        }
    },

    clearExpireFileCache(callBack){
        if (Platform.OS == 'ios'){
            oss.clearExpireFileCache((err)=>{
                callBack();
            })
        }else{
            oss.clearExpireFileCache().then((err)=>{
                callBack();
            })
        }
    },
    clearAllCache(callBack){
        if (Platform.OS == 'ios'){
            oss.clearAllCache((err)=>{
                callBack();
            })
        }else{
            oss.clearAllCache.then((err)=>{
                callBack();
            })
        }
    },
    getCacheSizeAll(callBack){
        if (Platform.OS == 'ios'){
            oss.getCacheSizeAll((err,size)=>{
                callBack(size);
            })
        }else{
            oss.getCacheSizeAll().then((ret)=>{
                callBack(ret);
            })
        }
    },
    getCacheSizeByPath(path,callBack){
        if (Platform.OS == 'ios'){
            oss.getCacheSizeByPath(path,(err,size)=>{
                callBack(size);
            })
        }else{
            oss.getCacheSizeByPath(path).then((ret)=>{
                callBack(ret);
            }).catch(()=>{
                callBack(0);
            })
        }
    },
    getTotalMemorySize(callBack){
        if (Platform.OS == 'ios'){
            oss.getTotalMemorySize((err,size)=>{
                callBack(size);
            })
        }else{
            oss.getTotalMemorySize().then((ret)=>{
                callBack(ret);
            })
        }
    },
    getCacheSizeTmp(callBack){
        if (Platform.OS == 'ios'){
            oss.getCacheSizeTmp((err,size)=>{
                callBack(size);
            })
        }else{
            oss.getCacheSizeTmp().then((ret)=>{
                callBack(ret);
            })
        }
    },
    upload(folder, filePath, progress, suc, fail) {
        let fileName = this._getFileName(this._getFileExt(filePath));
        const uploadProgress = (p) => {
            if (progress) {
                progress(p);
            }
        };
        this._addListener('eventUploadProgress', uploadProgress);
        oss.uploadSingle(filePath, folder + '/' + fileName).then((fileKey) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            suc(fileKey);
        }).catch((e) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            if (fail) {
                fail(e);
            }
        });
    },
    uploadMul(folder, filePaths, progress, suc,fail) {
        let fileNames = [];
        for (let i = 0; i < filePaths.length; i++) {
            fileNames.push(folder + '/' + this._getFileName(this._getFileExt(filePaths[i])));
        }
        const uploadProgress = (p) => {
            if (progress) {
                progress(p);
            }
        };
        this._addListener('eventUploadProgress', uploadProgress);
        oss.uploadMul(filePaths, fileNames).then((fileKeys) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            suc(fileKeys);
        }).catch((e) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            if (fail) {
                fail(e);
            }
        });
    },
    uploadCompress(folder, filePath, progress, suc, fail) {
        let ext = this._getFileExt(filePath);
        this.uploadCompressExt(folder,filePath,ext,progress,suc,fail);
    },
    uploadCompressExt(folder, filePath, fileExt, progress, suc, fail) {
        let ext = fileExt;
        let type = this._getFileTypeByExt(ext);
        if (type === -1){
            if (fail){
                fail(null);
            }
            return;
        }
        let fileName = this._getFileName(ext);
        const uploadProgress = (p) => {
            if (progress) {
                progress(p);
            }
        };
        this._addListener('eventUploadProgress', uploadProgress);
        oss.uploadSingleCompress(filePath,type, folder + '/' + fileName).then((fileKey) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            suc(fileKey);
        }).catch((e) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            if (fail) {
                fail(e);
            }
        });
    },
    uploadMulCompress(folder, filePaths, progress, suc,fail) {
        let fileExts = [];
        for (let i = 0; i < filePaths.length; i++) {
            let ext = this._getFileExt(filePaths[i]);
            fileExts.push(ext);
        }
        this.uploadMulCompressExt(folder,filePaths,fileExts,progress,suc,fail);
    },
    uploadMulCompressExt(folder, filePaths,fileExts, progress, suc,fail) {
        let fileNames = [];
        let fileTypes = [];
        for (let i = 0; i < filePaths.length; i++) {
            filePaths[i] = filePaths[i].replace('file://', '');
            let ext = fileExts[i];
            fileTypes.push(this._getFileTypeByExt(ext));
            fileNames.push(folder + '/' + this._getFileName(this._getFileExt(filePaths[i])));
        }
        const uploadProgress = (p) => {
            if (progress) {
                progress(p);
            }
        };
        this._addListener('eventUploadProgress', uploadProgress);
        oss.uploadMulCompress(filePaths,fileTypes, fileNames).then((fileKeys) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            suc(fileKeys);
        }).catch((e) => {
            this._removeListener('eventUploadProgress', uploadProgress);
            if (fail) {
                fail(e);
            }
        });
    },
    downloadBySendProgress(tag,ossFile,expireTime, suc,fail){
        let fileExt = this._getFileExt(ossFile);

        oss.download(tag+'',ossFile,fileExt,expireTime).then((filePath) => {
            suc(filePath);
        }).catch((e) => {
            if (fail) {
                fail(e);
            }
        })
    },
    downloadBySendProgressAndExt(tag,ossFile,expireTime,ext, suc,fail){
        let fileExt = ext;

        oss.download(tag+'',ossFile,fileExt,expireTime).then((filePath) => {
            suc(filePath);
        }).catch((e) => {
            if (fail) {
                fail(e);
            }
        })
    },
    download(tag,ossFile,expireTime,progress, suc,fail) {
        let fileExt = this._getFileExt(ossFile);
        const downloadProgress = (p) => {
            if (progress) {
                progress(p);
            }
        };
        this._addListener('eventDownloadProgress', downloadProgress);
        oss.download(tag+'',ossFile,fileExt,expireTime).then((filePath) => {
            this._removeListener('eventDownloadProgress', downloadProgress);
            suc(filePath);
        }).catch((e) => {
            this._removeListener('eventDownloadProgress', downloadProgress);
            if (fail) {
                fail(e);
            }
        });
    },
    _getFileTypeByExt(ext) {
        let imgExt = ['png','jpg','gif','bmp','jpeg'];
        let videoExt = ['mp4','mov'];
        for (let i = 0; i < imgExt.length; i++){
            let type = imgExt[i];
            if (ext === type || ext === type.toUpperCase()){
                return 0;
            }
        }
        for (let i = 0; i < videoExt.length; i++){
            let type = videoExt[i];
            if (ext === type || ext === type.toUpperCase()){
                return 1;
            }
        }
        return -1;
    },
    _getFileExt(filepath) {
        if (filepath !== "") {
            if (filepath.indexOf(".") == -1){
                return '';
            }
            let pos = filepath.replace(/.+\./, "");
            return pos;
        }
        return '';
    },
    _addListener(type, handler) {
        let listener;
        if (Platform.OS === 'ios') {
            const Emitter = new NativeEventEmitter(oss);
            listener = Emitter.addListener(
                type,
                (data) => {
                    handler(data);
                }
            );
        }
        else {
            listener = NativeAppEventEmitter.addListener(
                type,
                (uploadData) => {
                    handler(uploadData);
                }
            );
        }
        _subscriptions.set(handler, listener);
    },
    _removeListener(type, handler) {
        let listener = _subscriptions.get(handler);
        if (!listener) {
            return;
        }
        listener.remove();
        _subscriptions.delete(handler);
    },

    //最终格式 (前半段)2017-11-1_(后半段)132132321312
    //获取文件名  文件名的前半部分
    _getFileName(ext) {
        var timestamp = new Date().getTime();
        var randNum = Math.floor(Math.random()*1000+1);
        var fileName = this._getNowFormatDate() + '_' + timestamp + '_' + randNum+ '.' + ext;
        return fileName;
    },

    _getNowFormatDate() {
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

};
module.exports = SFAliyunOss;
