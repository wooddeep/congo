# quick start
***


### 代码克隆
#git clone https://github.com/wooddeep/congo  
#cd congo  
可以发现在congo目录下有以下文件夹和文件：     
#ls -l  
-rw-r--r-- 1 lihan 197609 10380 Sep  5 05:42 README.md  
drwxr-xr-x 1 lihan 197609     0 Sep  5 05:40 conf/  
-rw-r--r-- 1 lihan 197609   547 Feb 19  2018 makefile  
-rw-r--r-- 1 lihan 197609 10770 Sep  3 03:03 pom.xml  
-rw-r--r-- 1 lihan 197609   200 Sep  5 06:25 quick start.md  
drwxr-xr-x 1 lihan 197609     0 Sep  4 09:16 res/  
drwxr-xr-x 1 lihan 197609     0 Sep  4 09:13 src/  

其中conf目录存放了程序的配置文件congfig.js及日志的配置文件log4j2.xml  

同时，程序也支持在命令中指定启动参数，以覆盖config.js的配置
 
### 编译打包
#mvn package  
在target目录下面生成相应的jar包

### 运行

#### 帮助命令
#java -jar target\congo-0.0.1-SNAPSHOT.jar -h

显示如下：  
Usage: java -jar /path-to-jar/congo-(version)-SNAPSHOT.jar [cmd]  
        -f      --conf  Set the configure file dir!  
        -h      --help  Print this usage information!  
        -t      --telport       Set the telnet server's port!  
        -r      --restart       Set the node's restart policy!  
        -l      --logdir        Set the log's directory!  
        -m      --master        Set the node as master!  
        -c      --cluster       Set the node's cluster mode when run as master!  
        -d      --proxyport     Set the master's proxy port! when node run as master mode!  
        -s      --mongod        Set the mongodb's address and port when node run as worker mode!  
        -H      --clusterHost   Set the cluster host!  
        -L      --clusterList   Set the cluster list!  
        -z      --zklist        Set the zookeeper list!  
其中:  
-f 指定配置文件所在的目录，如果不指定的话，默认为./conf    
-m 设置节点是否以proxy模式运行，默认为false及为agent模式；  
-d 当设置节点为proxy模式时，-d设置mongo代理服务的端口；  
-s 当设置节点为agent模式是，-s设置被agent代表的mongodb的地址和端口；  
-z 设置系统zookeeper集群的列表  

#### 以proxy模式启动节点(master节点, 假设ip地址为10.144.0.30)
java -jar target\congo-0.0.1-SNAPSHOT.jar -m true -f ./conf -d 57017

#### 以agent模式启动节点(slave节点)
java -jar target\congo-0.0.1-SNAPSHOT.jar -f ./conf -d 112.74.167.132:27017  

### 连接代理
mongo 10.144.0.30:57017  

 
        