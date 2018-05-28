//
//  SFAliyunOssFIleCache.h
//  SFAliyunOss
//
//  Created by rw on 2018/5/25.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface SFAliyunOssFileCache : NSObject
+(id)share;
//生成过期时间的名字
- (NSString *)createFileName:(NSString *)ext expireTime:(int)expireTime;
//删除过期文件
- (void)clearExpireFileCache;
//删除所有文件
- (void)clearAllCache;
//获取非永久保存文件的总大小  单位btye
- (int)getCacheSizeTmp;
//获取所有文件的总大小  单位btye
- (int)getCacheSizeAll;
//输入缓存中所有文件
- (void)printCacheFiles;
@end
