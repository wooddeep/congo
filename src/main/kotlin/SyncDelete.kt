/**
 * Created by lihan on 2018/7/13.
 */
/**
 * Created by lihan on 2018/7/13.
 */
/**
 * Created by lihan on 2018/7/13.
 */

package com.migu.tsg

import com.migu.sdk.protocol.DeleteBody
import com.migu.sdk.protocol.MongoBody
import com.migu.sdk.protocol.MongoHead
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

// java -jar target\congo-0.0.1-SNAPSHOT.jar -W true -c ../conf

class SyncDelete : SyncHandle() {
    override fun handle(vertx: Vertx, head: MongoHead, body: MongoBody, socket: NetSocket) {
        launch(vertx.dispatcher()) {
            val delete = body as DeleteBody
            val dbcoll = delete.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_WORKER_LIST", "", h) }.body()
            val workerList = resp.getJsonArray("data", JsonArray())
            if (workerList.size() == 0) return@launch // 当前无工作的节点, 显然无法删除数据

            // 查询collections对应的slice列表, TODO 从zookeepr中转存到本地缓存
            resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_SUB_COLLECTION_LIST", String.format("/%s/%s", dbcoll[0], dbcoll[1]), h) }.body()
            var sliceArr = resp.getJsonArray("data", JsonArray())
            for (slice in sliceArr) {
                var json = slice as JsonObject
                val option = JsonObject()
                option.put("db", dbcoll[0])
                option.put("coll", dbcoll[1] + json.getString("name"))
                option.put("query", delete.selector.toJson())
                var master = json.getJsonObject("data").getString("master")
                var count = awaitResult<Message<Long>> { h -> vertx.eventBus().send(master + ":delete", option, h) }.body() // TODO update the count in zookeeper!
                println("## delete count: ${count}")
            }
        }
    }
}