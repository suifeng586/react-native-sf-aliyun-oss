//
//  RCTAliyunOSS.m
//  RCTAliyunOSS
//
//  Created by 李京生 on 2016/10/26.
//  Copyright © 2016年 lesonli. All rights reserved.
//

#import "SFAliyunOssBridge.h"
#import <Foundation/Foundation.h>
#import "SFAliyunOss.h"
@implementation SFAliyunOssBridge{
  
}
-(NSArray<NSString *> *)supportedEvents{
  return @[@"eventUploadProgress",@"eventDownloadProgress"];
}
-(void)sendUploadProgress:(int64_t) bytes totalByte:(int64_t) totalByte totalBytesExpected:(int64_t) totalBytesExpected progress:(float)progress{
  [self sendEventWithName:@"eventUploadProgress" body:@{
                                                        @"bytes":[NSString stringWithFormat:@"%lld",bytes],
                                                        @"totalByte":[NSString stringWithFormat:@"%lld",totalByte],
                                                        @"totalBytesExpected":[NSString stringWithFormat:@"%lld",totalByte],
                                                        @"progress":[NSString stringWithFormat:@"%f",progress]}];
}
-(void)sendDownloadProgress:(NSString *)tag bytes:(int64_t) bytes totalByte:(int64_t) totalByte totalBytesExpected:(int64_t) totalBytesExpected progress:(float)progress{
  [self sendEventWithName:@"eventDownloadProgress" body:@{
                                                        @"tag":tag,
                                                        @"bytes":[NSString stringWithFormat:@"%lld",bytes],
                                                        @"totalByte":[NSString stringWithFormat:@"%lld",totalByte],
                                                        @"totalBytesExpected":[NSString stringWithFormat:@"%lld",totalByte],
                                                        @"progress":[NSString stringWithFormat:@"%f",progress]}];
}
RCT_EXPORT_MODULE()


// 由阿里云颁发的AccessKeyId/AccessKeySecret初始化客户端。
// 明文设置secret的方式建议只在测试时使用，
// 如果已经在bucket上绑定cname，将该cname直接设置到endPoint即可
RCT_EXPORT_METHOD(initWithKey:(NSString *)accessKey
                  secretKey:(NSString *)secretKey
                  securityToken:(NSString *)securityToken
                  endpoint:(NSString *)endpoint
                  bucketName:(NSString *)bucketName)
{
    
  [[SFAliyunOss share] initWithKey:accessKey secretKey:secretKey securityToken:securityToken endpoint:endpoint bucketName:bucketName];
   
}
RCT_EXPORT_METHOD(setImageMaxSize:(int)size)
{
  //单位kb
  [[SFAliyunOss share] setImageMaxSize:size];
}

RCT_EXPORT_METHOD(deleteFile:(NSString*)filePath callback:(RCTResponseSenderBlock)callback) {
  
  [[SFAliyunOss share] deleteFile:filePath sucessBlock:^(NSString *fileKey) {
    callback(@[[NSNull null],fileKey]);
  }];
}
RCT_REMAP_METHOD(download,
                 tag:(NSString*)tag
                 ossFile:(NSString *)ossFile
                 fileExt:(NSString *)fileExt
                 expireTime:(NSInteger)expireTime
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  
  [[SFAliyunOss share] download:ossFile fileExt:fileExt expireTime:expireTime progressBlock:^(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected) {
    float progress = ((float)totalByte)/totalBytesExpected;
    [self sendDownloadProgress:tag bytes:bytes totalByte:totalByte totalBytesExpected:totalBytesExpected progress:progress];
  } sucessBlock:^(NSString *fileKey) {
    resolve(fileKey);
  } failBlock:^(NSError *error) {
    reject(@"-1", [error description], nil);
  }];
}

RCT_REMAP_METHOD(uploadSingle,
                  sourceFile:(NSString *)sourceFile
                  ossFile:(NSString *)ossFile
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    
  [[SFAliyunOss share] uploadSingleByPath:sourceFile ossFile:ossFile progressBlock:^(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected) {
    float progress = ((float)totalByte)/totalBytesExpected;
    [self sendUploadProgress:bytes totalByte:totalByte totalBytesExpected:totalBytesExpected progress:progress];
  } sucessBlock:^(NSString *fileKey) {
    resolve(fileKey);
  } failBlock:^(NSError *error) {
    reject(@"-1", [error description], nil);
  }];
}

RCT_REMAP_METHOD(uploadMul,
                 sourceFiles:(NSArray *)sourceFiles
                 ossFiles:(NSArray *)ossFiles
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  
  [[SFAliyunOss share] uploadMulByPath:sourceFiles ossFiles:ossFiles progressMulBlock:^(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected, float progress) {
    [self sendUploadProgress:bytes totalByte:totalByte totalBytesExpected:totalBytesExpected progress:progress];
  } sucessMulBlock:^(NSArray *fileKeys) {
    resolve(fileKeys);
  } failMulBlock:^(NSError *error) {
    reject(@"-1", [error description], nil);
  }];
}

RCT_REMAP_METHOD(uploadSingleCompress,
                 sourceFile:(NSString *)sourceFile
                 fileType:(NSInteger)fileType
                 ossFile:(NSString *)ossFile
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  
  [[SFAliyunOss share] uploadSingleByPathCompress:sourceFile fileType:fileType ossFile:ossFile progressBlock:^(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected) {
    float progress = ((float)totalByte)/totalBytesExpected;
    [self sendUploadProgress:bytes totalByte:totalByte totalBytesExpected:totalBytesExpected progress:progress];
  } sucessBlock:^(NSString *fileKey) {
    resolve(fileKey);
  } failBlock:^(NSError *error) {
    reject(@"-1", [error description], nil);
  }];
}

RCT_REMAP_METHOD(uploadMulCompress,
                 sourceFiles:(NSArray *)sourceFiles
                 fileTypes:(NSArray *)fileTypes
                 ossFiles:(NSArray *)ossFiles
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject) {
  
  [[SFAliyunOss share] uploadMulByPathCompress:sourceFiles fileTypes:fileTypes ossFiles:ossFiles progressMulBlock:^(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected, float progress) {
    [self sendUploadProgress:bytes totalByte:totalByte totalBytesExpected:totalBytesExpected progress:progress];
  } sucessMulBlock:^(NSArray *fileKeys) {
    resolve(fileKeys);
  } failMulBlock:^(NSError *error) {
    reject(@"-1", [error description], nil);
  }];
}

RCT_EXPORT_METHOD(clearExpireFileCache:(RCTResponseSenderBlock)callback)
{
  //单位kb
  [[SFAliyunOss share].cache clearExpireFileCache];
  callback(@[[NSNull null]]);
}
RCT_EXPORT_METHOD(clearAllCache:(RCTResponseSenderBlock)callback)
{
  //单位kb
  [[SFAliyunOss share].cache clearAllCache];
  callback(@[[NSNull null]]);
}
RCT_EXPORT_METHOD(getCacheSizeAll:(RCTResponseSenderBlock)callback)
{
  //单位kb
  unsigned long long size = [[SFAliyunOss share].cache getCacheSizeAll];
  callback(@[[NSNull null],[NSString stringWithFormat:@"%lld",size]]);
}
RCT_EXPORT_METHOD(getTotalMemorySize:(RCTResponseSenderBlock)callback)
{
  //单位kb
  unsigned long long size = [[SFAliyunOss share].cache getTotalMemorySize];
  callback(@[[NSNull null],[NSString stringWithFormat:@"%lld",size]]);
}
RCT_EXPORT_METHOD(getCacheSizeTmp:(RCTResponseSenderBlock)callback)
{
  //单位kb
  unsigned long long size = [[SFAliyunOss share].cache getCacheSizeTmp];
  callback(@[[NSNull null],[NSString stringWithFormat:@"%lld",size]]);
}
RCT_EXPORT_METHOD(getCacheSizeByPath:(NSString*)path callback:(RCTResponseSenderBlock)callback)
{
  //单位kb
  unsigned long long size = [[SFAliyunOss share].cache getCacheSizeByPath:path];
  callback(@[[NSNull null],[NSString stringWithFormat:@"%lld",size]]);
}

@end
