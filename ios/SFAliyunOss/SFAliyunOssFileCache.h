//
//  SFAliyunOssFIleCache.h
//  SFAliyunOss
//
//  Created by rw on 2018/5/25.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface SFAliyunOssFileCache : NSObject
+(SFAliyunOssFileCache *)share;
//生成过期时间的名字
- (NSString *)createFileName:(NSString *)ext expireTime:(int)expireTime;
//删除过期文件
- (void)clearExpireFileCache;
//删除所有文件
- (void)clearAllCache;
//获取非永久保存文件的总大小  单位btye
- (unsigned long long)getCacheSizeTmp;
//获取所有文件的总大小  单位btye
- (unsigned long long)getCacheSizeAll;
//获取单个文件大小
- (unsigned long long)getCacheSizeByPath:(NSString *)filePath;
//输入缓存中所有文件
- (void)printCacheFiles;
- (unsigned long long)getTotalMemorySize;
@end
