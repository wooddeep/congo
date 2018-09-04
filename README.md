# congo
可动态扩容的分布式MONGODB透明代理

# 1. 背景
## 1.1 使用场景
MONGODB安装简单，使用灵活，无需事先创建数据库和表，且schema free，适合需求变动频繁，数据模型无法确定的场景。天然适合用于JSON格式的日志存储。在数据分析方面支持JS语法的query，支持基于map-reduce的分析方法。故其非常适合小型开发团队用于JSON格式日志的存储与分析工作。

## 1.2 分片方案
当采用MONGODB用于海量日志存储的时候，有两种已有可选方式：
### 1) 应用分表
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/sharding_by_app.png "应用分表")  
<center>图1. 单机分库分表</center> 
在此模式之下，应用程序控制分库、分表，例如可以按照日期作为表名，每一天产生一个新表。当已有物理节点的磁盘空间不足时，添加新的物理节点，在新的物理节点上启动MONGO服务，应用程序连上新的MONGO服务，并按日期创建新表。当添加新的数据库时，应用程序需要维持新的数据库连接，如图1所示mongoclient0，mongoclient1。

### 2)集群分片
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/sharding_by_mongo.png "应用分表")   
图2. 集群分片模式  
MONGODB本身支持shard的集群模式，在该模式下，由四种角色组成：mongos、config server、shard、replica set。mongos，数据库集群请求的入口，所有的请求都通过mongos进行协调，不需要在应用程序添加一个路由选择器，mongos自己就是一个请求分发中心，它负责把对应的数据请求转发到对应的shard服务器上；config server，顾名思义为配置服务器，存储所有数据库元信息（路由、分片）的配置。 mongos本身没有物理存储分片服务器和数据路由信息；shard，就是数据分片；Replicat set为shard的复制集，保证了分片数据的高可用。

## 1.3 方案对比
### 1)	应用分片
优点：部署简单，零配置，运维简单。  
缺点：应用程序控制分表，当新增MONGODB节点时，需要修改应用程序，以建立新的数据库连接；数据分析时，需要首先确定表的范围再分析，例如以日期分表的情况下，需要以日期范围确定表名；无高可用机制，需要人力监测各工作节点的工作状态，保障各节点可用。
###2)	集群分片
优点：原生支持，容量可以无限扩展，复制机制保障数据高可用，分片对应用程序透明。  
缺点：部署复杂，配置麻烦；当加入新的分片时，需要重新配置分片信息，另外需要数据的rebalance，如下图所示：  
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/sharding_dat_dis.png "应用分表")  
图3. 不同分片下，数据的分布  
可见，随着集群内分片数据的增加，需要迁移数据到不同的分片。

## 1.4 改进方案
设计一种分布式MONGODB的透明代理，该代理可以挂接若干MONGODB节点。应用程序通过代理创建的数据库和表及分片信息被存储于元数据服务器，应用程序通过代理写入的数据被代理存储于各分片中，应用程序通过代理读取数据时，代理整合各分片的数据返回给应用程序。当需要扩容时，新的MONGODB节点可以动态的注册入代理列表，而代理发现有新的节点加入，在新加入节点中创建分片，自动把新插入的数据放在新的节点分片中，这样就实现了数据的平衡，不需要rebalance。该方案结合应用分片和集群分片的优点：部署方便，配置简单，动态扩容，应用程序不用关心分片。

# 2.	整体架构
如图4所示，该系统分为三个部分：proxy集群，zookeeper集群，worker集群。  
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/framework.png "应用分表")  
图4. 改进集群方案  

### 1)	worker集群
work集群由worker节点组成，而worker节点由两部分组成：mongod、agent，其中mongod就是原生的MONGODB服务，agent为mongod代表，需要开发实现。agent对内（worker内）检查mongodb的健康状况，对外向zookeeper集群注册mongod信息，并且和zookeeper保持长连接来标识mongod的存活。同时agent需要观察其他worker的上下线情况。另外，agent通过检测mongo的操作日志，来同步主分片的数据到从分片。

### 2)	zookeeper集群
zookeeper集群为元数据服务集群，zookeeper集群通过PAXAS协议，保证了集群的高可用、以及数据的一致性。Zookeeper以类似文件系统树形结构的方式来存储数据，在该系统中，zookeeper主要保持两类数据：在线工作节点列表信息, 数据库分片信息。  
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/worker_list.png "应用分表")  
图5. 在线工作节点信息  
如图5所示，/worker目录下为在线工作节点列表，各节点为临时节点。当每一个工作节点启动时，agent会调用zookeeper的接口，在/worker目录下创建自己的mongod信息，并且会和zookeeper保持连接，如果worker因掉线和zookeeper失去连接，则对应的节点会从/worker目录下面删除，其他的work或者proxy可以观察/worker目录以侦听工作节点的上线或离线。  
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/metadata.png "应用分表")
图6. 数据库分片信息  
如图6所示，/db目录下存储数据库及分片信息，db_name为某一数据库的名称，collection为数据库db_name下面的某一集合（类似于mysql表）名称，slice0~2为集合collection的分片，其中slice0的内容如下：  
{  
    ”name”: ”collection0”,  	
	”count”:100000,  
	”master”:”10.144.0.29:27017”,  
	”slave”:”10.144.0.30:27017”  
}  
其中name字段代表着集合分片在每个worker的mongod中的实际集合名称，count字段代表着该分片中的数据条数，master代表着该分片的主数据存储位置，slave代表着该分片的备份数据存储位置，一个分片存储在两个位置用于实现数据的高可用。
  
### 3)	proxy集群
proxy集群为无状态的服务，可以横向扩展，以增加整个系统的并发性能。它对应用程序提供mongo协议的服务，在应用程序看来，它就是一个MONGODB； 如上述树形结构存储的数据，通过proxy查询MONGODB的信息，可以得出如下信息：
	数据库列表：
db_name，db_namex，db_namey
	数据库db_name的集合列表：
collection，collectionx，collectiony
也就是说，在应用程序看来，数据库db_name中有一个名称为collection的集合，但是该集合映却被拆分成colleciton0~n个分片，且各分片被实际存储于各个worker的mongod中。

## 3.1 插入数据
Step1. 解析mongo协议，获取数据库名称，集合名称，及待插入数据；  
Step2. 通过数据库名称和集合名称查询zookeeper中的分片树，获取对应的分片列表；  
Step3. 获取在线的worker列表；  
Step4. 遍历分片列表信息，查询记录数count值小于设置最大值（说明该分片还有插入空间），且master或slave的在线的分片；如果存在满足条件的分片，则进入Step5，否则进入Step6；  
Step5. 插入数据到master分片或者slave分片，更新zookeeper中分片信息的count值；  
Step6. 从在线worker列表中找到两个worker，分别作为新分片的master和slave节点，在master节点中创建master分片，把数据插入master分片之上，把分片的描述信息（count信息，master信息，slave信息）插入到zookeeper的分片树中，slave分片的插入由agent进行日志同步时插入。  
Step6. 插入结果返回给应用程序客户端  

## 3.2 查询数据
Step1. 解析mongo协议，获取数据库名称，集合名称，查询条件；  
Step2. 通过数据库名称和集合名称查询zookeeper中的分片树，获取对应的分片列表；  
Step3. 获取在线的worker列表；  
Step4. 遍历分片列表信息，获取各分片的master和slave地址，若mater在线，则记录对应master地址到待查列表，若master离线而slave在线，则记录对应slave到待查列表，若master和slave都不在线，则记录错误日志。  
Step5. 以Step4中生成的待查地址列表，启动线程或者协程向各个地址列表，发送查询命令；  
Step6. 整合Step5中各线程或协程的返回结果，混合数据为mongo协议的返回，反馈给应用客户端。   

## 3.3 修改数据
Step1. 解析mongo协议，获取数据库名称，集合名称，修改条件，修改数据；  
Step2. 通过数据库名称和集合名称查询zookeeper中的分片树，获取对应的分片列表；  
Step3. 获取在线的worker列表；  
Step4. 遍历集合对应的分片，获取各分片的master和slave地址，若mater在线，则记录对应master地址到待查列表，若master离线而slave在线，则记录对应slave到待查列表，若master和slave都不在线，则记录错误日志。  
Step5. 以Step4中生成的待查地址列表，启动线程或者协程向各个地址列表，发送修改命令；  
Step6. 整合Step5中各线程或协程的返回结果，混合数据为mongo协议的返回，反馈给应用客户端。  

## 3.4 删除数据
Step1. 解析mongo协议，获取数据库名称，集合名称，修改条件，修改数据；  
Step2. 通过数据库名称和集合名称查询zookeeper中的分片树，获取对应的分片列表；  
Step3. 获取在线的worker列表；  
Step4. 遍历集合对应的分片，获取各分片的master和slave地址，若mater在线，则记录对应master地址到待查列表，若master离线而slave在线，则记录对应slave到待查列表，若master和slave都不在线，则记录错误日志。  
Step5. 以Step4中生成的待查地址列表，启动线程或者协程向各个地址列表，发送删除命令；  
Step6. 整合Step5中各线程或协程的返回结果，混合数据为mongo协议的返回，反馈给应用客户端。  

# 4. 数据备份
TODO  
