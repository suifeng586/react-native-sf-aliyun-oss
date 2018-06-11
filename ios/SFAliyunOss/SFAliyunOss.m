//
//  SFAliyunOss.m
//  SFAliyunOss
//
//  Created by rw on 2018/5/25.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import "SFAliyunOss.h"
#import "OSSService.h"
#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>

static SFAliyunOss *_aliyun_oss;
@implementation SFAliyunOss{
  OSSClient *client;
  NSString *_bucketName;
  NSMutableArray *_fileKeys;
  NSInteger _imgMaxSize;
}
+(SFAliyunOss *)share{
  if (!_aliyun_oss){
    _aliyun_oss = [SFAliyunOss new];
  }
  return _aliyun_oss;
}
-(id)init{
  self = [super init];
  if (self){
    self.cache = [SFAliyunOssFileCache share];
    _imgMaxSize = 200*1000;//200k
  }
  return self;
}
-(void)setImageMaxSize:(int)size{
  _imgMaxSize = size*1000;
}
-(void)initWithKey:(NSString *)accessKey secretKey:(NSString *)secretKey securityToken:(NSString *)securityToken endpoint:(NSString *)endpoint bucketName:(NSString *)bucketName{
  id<OSSCredentialProvider> credential = [[OSSStsTokenCredentialProvider alloc] initWithAccessKeyId:accessKey secretKeyId:secretKey securityToken:securityToken];
  _bucketName = bucketName;
  //bucket上绑定cname，将该cname直接设置到endPoint
  OSSClientConfiguration * conf = [OSSClientConfiguration new];
  conf.maxRetryCount = 3; // 网络请求遇到异常失败后的重试次数
  conf.timeoutIntervalForRequest = 30; // 网络请求的超时时间
  conf.timeoutIntervalForResource = 24 * 60 * 60; // 允许资源传输的最长时间

  
  client = [[OSSClient alloc] initWithEndpoint:endpoint credentialProvider:credential clientConfiguration:conf];
}
- (void) compressImage:(NSString *) filePath finishBlock:(void(^)(NSData *imageData))finishBlock{
  UIImage *image = [UIImage imageWithContentsOfFile:filePath];
  NSData *oriData = UIImagePNGRepresentation(image);
  NSInteger oriSize = [oriData length];
  
  if (oriSize > _imgMaxSize){
    float scale = ((float)_imgMaxSize)/oriSize;
    NSData *data = UIImageJPEGRepresentation(image, scale);
    finishBlock(data);
  }else{
    finishBlock(oriData);
  }
  
}
- (void) compressVideo:(NSString *) filePath finishBlock:(void(^)(NSData *videoData))finishBlock
{
  NSString *outputPath = [self.cache createFileName:@"mp4" expireTime:30];
  //转码配置
  AVURLAsset *asset = [AVURLAsset URLAssetWithURL:[NSURL fileURLWithPath:filePath] options:nil];
  AVAssetExportSession *exportSession= [[AVAssetExportSession alloc] initWithAsset:asset presetName:AVAssetExportPresetMediumQuality];
  exportSession.shouldOptimizeForNetworkUse = YES;
  exportSession.outputURL = [NSURL fileURLWithPath:outputPath];
  exportSession.outputFileType = AVFileTypeMPEG4;
  [exportSession exportAsynchronouslyWithCompletionHandler:^{
    int exportStatus = exportSession.status;
    switch (exportStatus)
    {
      case AVAssetExportSessionStatusFailed:
      {
        // log error to text view
        NSError *exportError = exportSession.error;
        NSLog(@"%@",exportError);
        finishBlock(nil);
        break;
      }
      case AVAssetExportSessionStatusCompleted:
      {
        NSData *data = [NSData dataWithContentsOfFile:outputPath];
        finishBlock(data);
      }
    }
  }];
  
}
-(void)uploadMulByPathCompress:(NSMutableArray *)sourcePaths fileTypes:(NSMutableArray *)fileTypes ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock{
  if (_fileKeys){
    [_fileKeys removeAllObjects];
  }else{
    _fileKeys = [NSMutableArray new];
  }
  [self uploadMulByPathCompressSub:0 sourcePaths:sourcePaths fileTypes:fileTypes ossFiles:ossFiles progressMulBlock:progressMulBlock sucessMulBlock:sucessMulBlock failMulBlock:failMulBlock];
}
-(void)uploadMulByPathCompressSub:(int)index sourcePaths:(NSMutableArray *)sourcePaths fileTypes:(NSArray *)fileTypes  ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock{
  NSInteger count = [sourcePaths count];
  if (index >= count){
    if (sucessMulBlock){
      sucessMulBlock(_fileKeys);
    }
  }
  float p_start = ((float)index)/count;
  [self uploadSingleByPathCompress:sourcePaths[index] fileType:[fileTypes[index] intValue] ossFile:ossFiles[index] progressBlock:^(int64_t bytesSent, int64_t totalByteSent, int64_t totalBytesExpectedToSend) {
    if (progressMulBlock){
      float p = ((float)totalByteSent)/totalBytesExpectedToSend;
      progressMulBlock(bytesSent,totalByteSent,totalBytesExpectedToSend,p_start+p/count);
    }
  } sucessBlock:^(NSString *fileKey) {
    [_fileKeys addObject:fileKey];
    [self uploadMulByPathCompressSub:index+1 sourcePaths:sourcePaths fileTypes:fileTypes ossFiles:ossFiles progressMulBlock:progressMulBlock sucessMulBlock:sucessMulBlock failMulBlock:failMulBlock];
  } failBlock:^(NSError *error) {
    if (failMulBlock){
      failMulBlock(error);
    }
  }];
}
-(void)uploadSingleByPathCompress:(NSString *)sourcePath fileType:(FileType)fileType ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock{
  
  if (fileType == FileTypeImage){
    [self compressImage:sourcePath finishBlock:^(NSData *imageData) {
      [self uploadSingleByData:imageData ossFile:ossFile progressBlock:progressBlock sucessBlock:sucessBlock failBlock:failBlock];
    }];
  }else if (fileType == FileTypeVideo){
    [self compressVideo:sourcePath finishBlock:^(NSData *videoData) {
      [self uploadSingleByData:videoData ossFile:ossFile progressBlock:progressBlock sucessBlock:sucessBlock failBlock:failBlock];
    }];
  }else{
    if (failBlock){
      failBlock(nil);
    }
    return;
  }
}
-(void)uploadSingleByPath:(NSString *)sourcePath ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock{
  
  OSSPutObjectRequest * put = [self createPut:sourcePath ossFile:ossFile progressBlock:progressBlock];
  
  OSSTask * putTask = [client putObject:put];
  
  [putTask continueWithBlock:^id _Nullable(OSSTask * _Nonnull task) {
    if (!task.error) {
      if (sucessBlock){
        sucessBlock(put.objectKey);
      }
    } else {
      if (failBlock){
        failBlock(task.error);
      }
    }
    return nil;
  }];
}

-(void)uploadMulByPath:(NSMutableArray *)sourcePaths ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock{
  if (_fileKeys){
    [_fileKeys removeAllObjects];
  }else{
    _fileKeys = [NSMutableArray new];
  }
  [self uploadMulByPathSub:0 sourcePaths:sourcePaths ossFiles:ossFiles progressMulBlock:progressMulBlock sucessMulBlock:sucessMulBlock failMulBlock:failMulBlock];
}
-(void)uploadMulByPathSub:(int)index sourcePaths:(NSMutableArray *)sourcePaths ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock{
  NSInteger count = [sourcePaths count];
  if (index >= count){
    if (sucessMulBlock){
      sucessMulBlock(_fileKeys);
    }
  }
  
  float p_start = ((float)index)/count;
  [self uploadSingleByPath:sourcePaths[index] ossFile:ossFiles[index] progressBlock:^(int64_t bytesSent, int64_t totalByteSent, int64_t totalBytesExpectedToSend) {
    if (progressMulBlock){
      float p = ((float)totalByteSent)/totalBytesExpectedToSend;
      progressMulBlock(bytesSent,totalByteSent,totalBytesExpectedToSend,p_start+p/count);
    }
  } sucessBlock:^(NSString *fileKey) {
    [_fileKeys addObject:fileKey];
    [self uploadMulByPathSub:index+1 sourcePaths:sourcePaths ossFiles:ossFiles progressMulBlock:progressMulBlock sucessMulBlock:sucessMulBlock failMulBlock:failMulBlock];
  } failBlock:^(NSError *error) {
    if (failMulBlock){
      failMulBlock(error);
    }
  }];
}


-(void)uploadSingleByData:(NSData *)sourceData ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock{
  
  OSSPutObjectRequest * put = [self createPut:sourceData ossFile:ossFile progressBlock:progressBlock];
  
  OSSTask * putTask = [client putObject:put];

  [putTask continueWithBlock:^id _Nullable(OSSTask * _Nonnull task) {
    if (!task.error) {
      if (sucessBlock){
        sucessBlock(put.objectKey);
      }
    } else {
      if (failBlock){
        failBlock(task.error);
      }
    }
    return nil;
  }];
}

-(void)uploadMulByData:(NSMutableArray *)sourceDatas ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock{
  if (_fileKeys){
    [_fileKeys removeAllObjects];
  }else{
    _fileKeys = [NSMutableArray new];
  }
  [self uploadMulByDataSub:0 sourceDatas:sourceDatas ossFiles:ossFiles progressMulBlock:progressMulBlock sucessMulBlock:sucessMulBlock failMulBlock:failMulBlock];
}

-(void)uploadMulByDataSub:(int)index sourceDatas:(NSMutableArray *)sourceDatas ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock{
  int count = [sourceDatas count];
  if (index >= count){
    if (sucessMulBlock){
      sucessMulBlock(_fileKeys);
    }
  }
  float p_start = ((float)index)/count;
  [self uploadSingleByData:sourceDatas[index] ossFile:ossFiles[index] progressBlock:^(int64_t bytesSent, int64_t totalByteSent, int64_t totalBytesExpectedToSend) {
    if (progressMulBlock){
      float p = ((float)totalByteSent)/totalBytesExpectedToSend;
      progressMulBlock(bytesSent,totalByteSent,totalBytesExpectedToSend,p_start+p/count);
    }
  } sucessBlock:^(NSString *fileKey) {
    [_fileKeys addObject:fileKey];
    [self uploadMulByDataSub:index+1 sourceDatas:sourceDatas ossFiles:ossFiles progressMulBlock:progressMulBlock sucessMulBlock:sucessMulBlock failMulBlock:failMulBlock];
  } failBlock:^(NSError *error) {
    if (failMulBlock){
      failMulBlock(error);
    }
  }];
}

-(OSSPutObjectRequest *)createPut:(id)source ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock{
  OSSPutObjectRequest * put = [OSSPutObjectRequest new];
  
  put.bucketName = _bucketName;
  put.objectKey = ossFile;
  if ([source isKindOfClass:[NSData class]]){
    put.uploadingData = source;
  }else{
    put.uploadingFileURL = [NSURL fileURLWithPath:source];
  }
  put.uploadProgress = progressBlock;
  return put;
}

-(void)download:(NSString*)fileKey fileExt:(NSString *)fileExt expireTime:(int)expireTime progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock{
  OSSGetObjectRequest * request = [OSSGetObjectRequest new];
  NSString *filePath = [self.cache createFileName:fileExt expireTime:expireTime];
  // 必填字段
  request.bucketName = _bucketName;
  request.objectKey = fileKey;
  request.downloadToFileURL = [NSURL URLWithString:filePath];
  // 可选字段
  request.downloadProgress = progressBlock;

  OSSTask * getTask = [client getObject:request];
  [getTask continueWithBlock:^id(OSSTask *task) {
    if (!task.error) {
      if (sucessBlock){
        sucessBlock(filePath);
      }
    } else {
      if (failBlock){
        failBlock(task.error);
      }
    }
    return nil;
  }];
}
-(void)deleteFile:(NSString *)filePath sucessBlock:(sucessBlock)sucessBlock{
  [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
  if (sucessBlock){
    sucessBlock(filePath);
  }
}
@end
