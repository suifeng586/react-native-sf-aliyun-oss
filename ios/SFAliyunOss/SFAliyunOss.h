//
//  SFAliyunOss.h
//  SFAliyunOss
//
//  Created by rw on 2018/5/25.
//  Copyright © 2018年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SFAliyunOssFileCache.h"
typedef void (^progressBlock)(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected);
typedef void (^progressMulBlock)(int64_t bytes, int64_t totalByte, int64_t totalBytesExpected, float progress);
typedef void (^sucessBlock)(NSString *fileKey);
typedef void (^sucessMulBlock)(NSArray *fileKeys);
typedef void (^failBlock)(NSError *error);

typedef NS_OPTIONS(NSInteger, FileType) {
  FileTypeImage  = 0,
  FileTypeVideo  = 1
};
@interface SFAliyunOss : NSObject

@property (nonatomic,strong) SFAliyunOssFileCache *cache;

+(SFAliyunOss *)share;
//oss初始化
-(void)initWithKey:(NSString *)accessKey secretKey:(NSString *)secretKey securityToken:(NSString *)securityToken endpoint:(NSString *)endpoint bucketName:(NSString *)bucketName;

//上传单个文件 data格式
-(void)uploadSingleByData:(NSData *)sourceData ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock;
//上传多个文件 data格式
-(void)uploadMulByData:(NSMutableArray *)sourceDatas ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock;

//上传单个文件 path格式
-(void)uploadSingleByPath:(NSString *)sourcePath ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock;
//上传多个文件 path格式
-(void)uploadMulByPath:(NSMutableArray *)sourcePaths ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock;

//上传单个文件 path格式 带压缩
-(void)uploadSingleByPathCompress:(NSString *)sourcePath fileType:(FileType)fileType ossFile:(NSString *)ossFile progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock;
//上传多个文件 path格式 带压缩
-(void)uploadMulByPathCompress:(NSMutableArray *)sourcePaths fileTypes:(NSMutableArray *)fileTypes ossFiles:(NSMutableArray *)ossFiles progressMulBlock:(progressMulBlock)progressMulBlock sucessMulBlock:(sucessMulBlock)sucessMulBlock failMulBlock:(failBlock)failMulBlock;

//下载文件
-(void)download:(NSString*)fileKey fileExt:(NSString *)fileExt expireTime:(int)expireTime progressBlock:(progressBlock)progressBlock sucessBlock:(sucessBlock)sucessBlock failBlock:(failBlock)failBlock;

-(void)setImageMaxSize:(int)size;
-(void)deleteFile:(NSString *)filePath sucessBlock:(sucessBlock)sucessBlock;
@end
