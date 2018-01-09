## 简介
这个库调用阿里云的云解析API做到动态域名解析，使用Java 1.5编写，可以放在路由器的openwrt系统中运行

参考了[阿里云SDK](https://github.com/aliyun/aliyun-openapi-java-sdk)的源码，并做精简

## 安装运行
1. 使用maven编译
    ```
    mvn package
    ```
2. 从aliyun_ddns/target中获取`aliyun_ddns.jar`，将其上传到路由器或服务器上
3. 在aliyun_ddns.jar同级目录中编写`config.properties`，内容如下：
    ```
    # 区域ID，一般填写cn-hangzhou即可
    regionId=<regionId>
    # 阿里云API访问ID
    accessKeyId=<accessKeyId>
    # 阿里云API访问秘钥
    secret=<secret>
    # 域名
    domainName=<example.com>
    # 匹配RR的正则表达式
    recordPattern=
    ```
    请参考[如何选择 RegionId](https://help.aliyun.com/knowledge_detail/43190.html?spm=5176.11065259.1996646101.searchclickresult.269c30b5par2K4)、[地域和可用区](https://help.aliyun.com/document_detail/40654.html?spm=5176.10695662.1996646101.1.42de3412ohrQBN)、[获取AccessKey](https://help.aliyun.com/document_detail/63724.html?spm=5176.doc52740.6.541.Z1fNDa)    
    `recordPattern`填写正则表达式，用来匹配RR的值。RR指的是二级url，如`@`、`www`、`wap`等。只有匹配了recordPattern的、而且是A记录的才会被匹配，只匹配第一个。如果不填写recordPattern的值，则会找到第一个A记录进行匹配
4. 运行aliyun_ddns.jar即可，
    ```
    java -jar aliyun_ddns.jar
    ```
    
## 代码逻辑
1. 从[DescribeDomainRecords接口](https://help.aliyun.com/document_detail/29776.html?spm=5176.doc29739.6.620.LMdHQJ)获取域名解析记录列表
2. 用正则表达式去匹配查找域名解析记录及其记录值（获取记录的ip）
3. 获取公网ip
4. 判断公网ip和域名解析记录的值是否相等，如果不等则使用[UpdateDomainRecord接口](https://help.aliyun.com/document_detail/29774.html?spm=5176.doc29776.6.618.OWxgZ1)去修改记录，否则不做修改

## 注意事项
1. 这个库只修改域名解析记录的值，不添加记录，因此需要先手动在阿里云云解析中增加记录
2. 必须要保证第一条域名解析记录是A记录(域名解析成ip)，因为这个程序只修改一个条记录并修改记录值为ip
3. 如果修改解析记录成功，则退出码（exit code）是`666`；不修改则是`0`，其他退出码代表出错。可以编写一个shell脚本来对退出码进行判断做特殊逻辑
4. 请不要在意我那蹩脚的英文日志，本来我是输出中文日志的，但我那破路由器跑java有乱码QAQ，我不知道怎么解决所以才改成英文日志的