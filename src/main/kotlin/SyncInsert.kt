/**
 * Created by lihan on 2018/7/13.
 */
/**
 * Created by lihan on 2018/7/13.
 */

package com.migu.tsg

import com.migu.sdk.constant.Constant
import com.migu.sdk.protocol.Insert
import com.migu.sdk.protocol.MongoBody
import com.migu.sdk.protocol.MongoHead
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch
import org.apache.logging.log4j.LogManager
import java.util.*

// java -jar target\congo-0.0.1-SNAPSHOT.jar -W true -c ../conf

// TODO insert 操作 需要 备份 slave

class SyncInsert : SyncHandle() {
    val logger = LogManager.getLogger("global")

    fun restruct(vertx: Vertx, head: MongoHead, body: MongoBody, socket: NetSocket) {
        launch(vertx.dispatcher()) {
            val insert = body as Insert
            val dbcoll = insert.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_WORKER_LIST", "", h) }.body()
            val workerList = resp.getJsonArray("data", JsonArray())
            if (workerList.size() == 0) return@launch // 当前无工作的节点, 显然无法插入数据

            // 查询collections对应的slice列表, TODO 从zookeepr中转存到本地缓存
            resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_SUB_COLLECTION_LIST", String.format("/%s/%s", dbcoll[0], dbcoll[1]), h) }.body()
            val found = resp.getJsonArray("data", JsonArray()).find { cell ->
                // 获取 目标集合分片
                var json = cell as JsonObject
                val count = json.getJsonObject("data").getInteger("count")
                if (count.toInt() < Constant.MAX_COUNT_ONE_COLLECTION) {
                    return@find true
                }
                return@find false
            } as JsonObject? //{"name":"0","data":{"count":0,"master":"/127.0.0.1:27017"}}

            // 若 (系统尚无相关集合分片) {则新集合名称为0} 或 (所有集合分片数据皆满) {则新集合名称为已有所有分片总数}
            val name = found?.getString("name", "0") ?: resp.getJsonArray("data", JsonArray()).size().toString()
            val newSlice = found == null

            // 从在线 worker 列表中选取master和slave, TODO 确定worker选择策略
            val worker = found?.getJsonObject("data")?.getString("master") ?: workerList.get<String>(0)
            // TODO 判断 master或者 slave是否离线
            var option = JsonObject().put("db", dbcoll[0]).put("coll", dbcoll[1] + name).put("data", insert.document.toJson())
            awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send(worker + ":insert", option, h) }.body() // TODO

            if (newSlice) {
                awaitResult<Message<JsonObject>> { h ->
                    val path = String.format("/%s/%s/%s", dbcoll[0], dbcoll[1], name) // 0号slice
                    val data = JsonObject().put("count", 0).put("master", worker)
                    val msg = JsonObject().put("path", path).put("data", data)
                    vertx.eventBus().send("ADD_COLLECTION_SLICE", msg, h)
                }.body()
            }

            awaitResult<Message<JsonObject>> { h ->
                val path = String.format("/%s/%s/%s", dbcoll[0], dbcoll[1], name)
                vertx.eventBus().send("INC_COLLECTION_SLICE", path, h)
            }.body()
        }
    }

    fun randIndex(range: Int): Pair<Int, Int> {
        if (range < 2) return Pair(0, -1)
        var mindex = Random().nextInt(range)
        var sindex = Random().nextInt(range)
        while (mindex == sindex) sindex = Random().nextInt(range)
        return Pair(mindex, sindex)
    }

    override fun handle(vertx: Vertx, head: MongoHead, body: MongoBody, socket: NetSocket) {
        launch(vertx.dispatcher()) {
            val insert = body as Insert
            val dbcoll = insert.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_WORKER_LIST", "", h) }.body()
            val workerList = resp.getJsonArray("data", JsonArray())
            if (workerList.size() == 0) return@launch // 当前无工作的节点, 显然无法插入数据

            // 查询collections对应的slice列表, TODO 从zookeepr中转存到本地缓存
            resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_SUB_COLLECTION_LIST", String.format("/%s/%s", dbcoll[0], dbcoll[1]), h) }.body()
            val found = resp.getJsonArray("data", JsonArray()).find { cell ->
                // 获取 目标集合分片
                var json = cell as JsonObject
                val count = json.getJsonObject("data").getInteger("count")
                if (count.toInt() < Constant.MAX_COUNT_ONE_COLLECTION) {
                    return@find true
                }
                return@find false
            } as JsonObject? //{"name":"0","data":{"count":0,"master":"/127.0.0.1:27017"}}

            // 若 (系统尚无相关集合分片) {则新集合名称为0} 或 (所有集合分片数据皆满) {则新集合名称为已有所有分片总数}
            val name = found?.getString("name", "0") ?: resp.getJsonArray("data", JsonArray()).size().toString()
            val newSlice = found == null
            var option = JsonObject().put("db", dbcoll[0]).put("coll", dbcoll[1] + name).put("data", insert.document.toJson())

            if (!newSlice) { // 已有集合
                val master = found?.getJsonObject("data")?.getString("master") ?: ""
                val slave = found?.getJsonObject("data")?.getString("slave") ?: ""
                val mlive = workerList.find { worker -> worker.equals(master) } != null
                val slive = workerList.find { worker -> worker.equals(slave) } != null

                if (mlive == true) {
                    val worker = found?.getJsonObject("data")?.getString("master") ?: workerList.get<String>(0)
                    awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send(worker + ":insert", option, h) }.body()
                    return@launch
                }
                if (slive == true) {
                    val worker = found?.getJsonObject("data")?.getString("slave") ?: workerList.get<String>(0)
                    awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send(worker + ":insert", option, h) }.body()
                    return@launch
                }
                logger.error("## all the workers of the collections slice is down! ")
                return@launch

            } else { // 新集合 已有的worker列表中选取master和slave
                var indexPair = randIndex(workerList.size())
                var mindex = indexPair.first
                var sindex = indexPair.second

                val master = found?.getJsonObject("data")?.getString("master") ?: workerList.get<String>(mindex)
                val data = JsonObject().put("count", 0).put("master", master)
                if (sindex >= 0) {
                    val slave = found?.getJsonObject("data")?.getString("slave") ?: workerList.get<String>(sindex)
                    data.put("slave", slave)
                }

                awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send(master + ":insert", option, h) }.body()
                awaitResult<Message<JsonObject>> { h ->
                    val path = String.format("/%s/%s/%s", dbcoll[0], dbcoll[1], name) // 0号slice
                    val msg = JsonObject().put("path", path).put("data", data)
                    vertx.eventBus().send("ADD_COLLECTION_SLICE", msg, h)
                }.body()
            }

            awaitResult<Message<JsonObject>> { h ->
                val path = String.format("/%s/%s/%s", dbcoll[0], dbcoll[1], name)
                vertx.eventBus().send("INC_COLLECTION_SLICE", path, h)
            }.body()
        }
    }
}