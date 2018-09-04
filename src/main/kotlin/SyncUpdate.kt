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

import com.migu.sdk.protocol.MongoBody
import com.migu.sdk.protocol.MongoHead
import com.migu.sdk.protocol.UpdateBody
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

// java -jar target\congo-0.0.1-SNAPSHOT.jar -W true -c ../conf

class SyncUpdate : SyncHandle() {

    suspend fun update(vertx: Vertx, dbcoll: Array<String>, body: MongoBody, resp: JsonObject) {
        return async(CommonPool) {
            val update = body as UpdateBody
            var sliceArr = resp.getJsonArray("data", JsonArray())
            for (slice in sliceArr) {
                var json = slice as JsonObject
                val option = JsonObject()
                option.put("db", dbcoll[0])
                option.put("coll", dbcoll[1] + json.getString("name"))
                option.put("query", update.selector.toJson())
                option.put("update", update.update.toJson())
                var master = json.getJsonObject("data").getString("master")
                var count = awaitResult<Message<Long>> { h -> vertx.eventBus().send(master + ":update", option, h) }.body()
                println("## update count: ${count}")
                if (count > 0) return@async
            }
        }.await()
    }

    suspend fun upsert(vertx: Vertx, dbcoll: Array<String>, body: MongoBody, resp: JsonObject) {
        return async(CommonPool) {
            val update = body as UpdateBody
            var sum = 0L
            var sliceArr = resp.getJsonArray("data", JsonArray())
            for (slice in sliceArr) { // 对全部的slice进行update, 若无一成功, 则插入某一slice
                var json = slice as JsonObject
                val option = JsonObject()
                option.put("db", dbcoll[0])
                option.put("coll", dbcoll[1] + json.getString("name"))
                option.put("query", update.selector.toJson())
                option.put("update", update.update.toJson())
                var master = json.getJsonObject("data").getString("master")
                var count = awaitResult<Message<Long>> { h -> vertx.eventBus().send(master + ":update:true", option, h) }.body()
                sum += count
            }

            if (sum == 0L) { // 未update, 则插入最新的slice
                val json = sliceArr.getJsonObject(sliceArr.size() -1)
                val master = json.getJsonObject("data").getString("master")
                val coll = dbcoll[1] + json.getString("name")
                var option = JsonObject().put("db", dbcoll[0]).put("coll", coll).put("data", update.update.toJson())
                awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send(master + ":insert", option, h) }.body() // TODO
            }

        }.await()
    }

    suspend fun mupdate(vertx: Vertx, dbcoll: Array<String>, body: MongoBody, resp: JsonObject) {
        return async(CommonPool) {
            val update = body as UpdateBody
            var sliceArr = resp.getJsonArray("data", JsonArray())
            for (slice in sliceArr) {
                var json = slice as JsonObject
                val option = JsonObject()
                option.put("db", dbcoll[0])
                option.put("coll", dbcoll[1] + json.getString("name"))
                option.put("query", update.selector.toJson())
                option.put("update", update.update.toJson())
                var master = json.getJsonObject("data").getString("master")
                var count = awaitResult<Message<Long>> { h -> vertx.eventBus().send(master + ":update:false:true", option, h) }.body()
                println("## update count: ${count}")
            }
        }.await()
    }

    suspend fun mupsert(vertx: Vertx, dbcoll: Array<String>, body: MongoBody, resp: JsonObject) {
        return async(CommonPool) {
            val update = body as UpdateBody
            var sum = 0L
            var sliceArr = resp.getJsonArray("data", JsonArray())
            for (slice in sliceArr) { // 对全部的slice进行update, 若无一成功, 则插入某一slice
                var json = slice as JsonObject
                val option = JsonObject()
                option.put("db", dbcoll[0])
                option.put("coll", dbcoll[1] + json.getString("name"))
                option.put("query", update.selector.toJson())
                option.put("update", update.update.toJson())
                var master = json.getJsonObject("data").getString("master")
                var count = awaitResult<Message<Long>> { h -> vertx.eventBus().send(master + ":update:false:true", option, h) }.body()
                sum += count
            }

            if (sum == 0L) { // 未update, 则插入最新的slice
                val json = sliceArr.getJsonObject(sliceArr.size() -1)
                val master = json.getJsonObject("data").getString("master")
                val coll = dbcoll[1] + json.getString("name")
                var option = JsonObject().put("db", dbcoll[0]).put("coll", coll).put("data", update.update.toJson())
                awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send(master + ":insert", option, h) }.body() // TODO
            }

        }.await()
    }

    override fun handle(vertx: Vertx, head: MongoHead, body: MongoBody, socket: NetSocket) {
        launch(vertx.dispatcher()) {
            val update = body as UpdateBody
            val dbcoll = update.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_WORKER_LIST", "", h) }.body()
            val workerList = resp.getJsonArray("data", JsonArray())
            if (workerList.size() == 0) return@launch // 当前无工作的节点, 显然无法update数据

            // 查询collections对应的slice列表, TODO 从zookeepr中转存到本地缓存
            resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_SUB_COLLECTION_LIST", String.format("/%s/%s", dbcoll[0], dbcoll[1]), h) }.body()
            val upsertFlag = (update.flags and 0x00000001) != 0
            val multiFlag = (update.flags and 0x00000002) != 0

            println("upsertFlag:${upsertFlag}, multiFlag:${multiFlag}")

            if (!upsertFlag && !multiFlag) { // 对各slice进行update操作, 某一slice update成功则退出
                async { update(vertx, dbcoll, body, resp) }
            }

            if (upsertFlag && !multiFlag) { // 对各slice进行update操作，若有1成功则退出, 若无1退出则对某一slice插入
                async { upsert(vertx, dbcoll, body, resp) }
            }

            if (!upsertFlag && multiFlag) {
                async { mupdate(vertx, dbcoll, body, resp) }
            }

            if (upsertFlag && multiFlag) {
                async { mupsert(vertx, dbcoll, body, resp) }
            }
        }
    }
}