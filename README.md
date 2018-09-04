# congo
可动态扩容的分布式MONGODB透明代理

# 1. 背景
## 1.1 使用场景
MONGODB安装简单，使用灵活，无需事先创建数据库和表，且schema free，适合需求变动频繁，数据模型无法确定的场景。天然适合用于JSON格式的日志存储。在数据分析方面支持JS语法的query，支持基于map-reduce的分析方法。故其非常适合小型开发团队用于JSON格式日志的存储与分析工作。

## 1.2 分片方案
当采用MONGODB用于海量日志存储的时候，有两种已有可选方式：
### 1) 应用分表
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/sharding_by_app.png "应用分表")  
图1. 单机分库分表  
在此模式之下，应用程序控制分库、分表，例如可以按照日期作为表名，每一天产生一个新表。当已有物理节点的磁盘空间不足时，添加新的物理节点，在新的物理节点上启动MONGO服务，应用程序连上新的MONGO服务，并按日期创建新表。当添加新的数据库时，应用程序需要维持新的数据库连接，如图1所示mongoclient0，mongoclient1。

### 2)集群分片
![应用分表](https://github.com/wooddeep/congo/blob/master/res/images/sharding_by_mongo.png "应用分表")   
图2. 集群分片模式  
MONGODB本身支持shard的集群模式，在该模式下，由四种角色组成：mongos、config server、shard、replica set。mongos，数据库集群请求的入口，所有的请求都通过mongos进行协调，不需要在应用程序添加一个路由选择器，mongos自己就是一个请求分发中心，它负责把对应的数据请求转发到对应的shard服务器上；config server，顾名思义为配置服务器，存储所有数据库元信息（路由、分片）的配置。 mongos本身没有物理存储分片服务器和数据路由信息；shard，就是数据分片；Replicat set为shard的复制集，保证了分片数据的高可用。
