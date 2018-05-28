//
//  SFAliyunOssFIleCache.m
//  SFAliyunOss
//
//  Created by rw on 2018/5/25.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import "SFAliyunOssFileCache.h"

#define FOLDER_OSS @"SF_OSS_FILES"
static SFAliyunOssFileCache *_oss_file_cache;
@implementation SFAliyunOssFileCache{
  NSString *_fileFloader;
}
+(id)share{
  if (!_oss_file_cache){
    _oss_file_cache = [SFAliyunOssFileCache new];
  }
  return _oss_file_cache;
}
-(id)init{
  self = [super init];
  if (self){
    _fileFloader = [self createFloader:FOLDER_OSS];
  }
  return self;
}
- (void)printCacheFiles{
  NSArray *files = [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:_fileFloader error:nil];
  for (NSString *name in files) {
    NSLog(@"SFAliyunOssCache:%@",name);
  }
}
- (int)getCacheSizeAll{
  NSArray *files = [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:_fileFloader error:nil];
  int size = 0;
  for (NSString *name in files) {
    size += [self fileSize:[_fileFloader stringByAppendingPathComponent:name]];
  }
  return size;
}
- (int)getCacheSizeTmp{
  NSArray *files = [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:_fileFloader error:nil];
  int size = 0;
  for (NSString *name in files) {
    NSArray *ary = [name componentsSeparatedByString:@"."];
    NSString *subName = ary[0];
    NSArray *timerAry = [subName componentsSeparatedByString:@"_"];
    if ([timerAry count] >= 2){
      int type = [timerAry[1] intValue];
      if (type == 1){//永久
        continue;
      }
    }
    size += [self fileSize:[_fileFloader stringByAppendingPathComponent:name]];
  }
  
  return size;
}
- (int)fileSize:(NSString *)filePath{
  NSDictionary *fileAttributeDic=[[NSFileManager defaultManager] attributesOfItemAtPath:filePath error:nil];
  return (int)fileAttributeDic.fileSize;
}
- (void)clearAllCache{
  NSArray *files = [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:_fileFloader error:nil];
  for (NSString *name in files) {
      [[NSFileManager defaultManager] removeItemAtPath:[_fileFloader stringByAppendingPathComponent:name] error:nil];
  }
}
- (void)clearExpireFileCache{
  NSArray *files = [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:_fileFloader error:nil];
  for (NSString *name in files) {
    NSArray *ary = [name componentsSeparatedByString:@"."];
    NSString *subName = ary[0];
    NSArray *timerAry = [subName componentsSeparatedByString:@"_"];
    NSString *timer = timerAry[0];
    if ([timerAry count] >= 2){
      int type = [timerAry[1] intValue];
      if (type == 1){//永久
        continue;
      }
    }
    
    NSTimeInterval ti = [timer doubleValue];
    NSTimeInterval c_ti = [self currentTimeInterval:0];
    if (c_ti >= ti){
      [[NSFileManager defaultManager] removeItemAtPath:[_fileFloader stringByAppendingPathComponent:name] error:nil];
    }
    
  }
}
- (NSString *)createFileName:(NSString *)ext expireTime:(int)expireTime{
  NSString *fileName = [NSString stringWithFormat:@"%@.%@",[self currentTimeStr:expireTime],ext];
  return [_fileFloader stringByAppendingPathComponent:fileName];
}
- (NSString *)documentDirectory{
  NSArray *directoryPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *documentDirectory = [directoryPaths objectAtIndex:0];
  return documentDirectory;
}
- (NSString *)createFloader:(NSString *)folder
{
  NSFileManager *fileManager = [NSFileManager defaultManager];
  NSString *floderPath = [[self documentDirectory] stringByAppendingPathComponent:folder];
  if (![fileManager fileExistsAtPath:floderPath]) {
    [fileManager createDirectoryAtPath:floderPath withIntermediateDirectories:YES attributes:nil error:nil];
  }
  return floderPath;
}
- (NSTimeInterval)currentTimeInterval:(int)addMin{
  NSDate* date = [NSDate dateWithTimeIntervalSinceNow:addMin];//获取当前时间0秒后的时间
  return [date timeIntervalSince1970]*1000;// *1000 是精确到毫秒，不乘就是精确到秒
}
- (NSString *)currentTimeStr:(int)expireTime{
  NSString *extStr = @"0";
  if (expireTime <= 0){
    expireTime = 0;
    extStr = @"1";
  }
  NSTimeInterval time=[self currentTimeInterval:expireTime*60];
  NSString *timeString = [NSString stringWithFormat:@"%.0f_%@", time,extStr];
  return timeString;
}
@end
