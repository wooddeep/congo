/**
 * Created by lihan on 2018/7/13.
 */

package com.migu.tsg

import com.migu.sdk.metadata.ControlInfo
import com.migu.sdk.protocol.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.apache.logging.log4j.LogManager
import org.bson.BSONObject
import org.bson.BasicBSONEncoder
import java.util.*

// java -jar target\congo-0.0.1-SNAPSHOT.jar -W true -c ../conf

class SyncFind : SyncHandle() {

    val logger = LogManager.getLogger("global")

    // {"name":"0","data":{"count":0,"master":"/127.0.0.1:27017"}}
    suspend fun findAsync(vertx: Vertx, query: Query, workerList: JsonArray, slice: JsonObject): ByteArray {
        return async(CommonPool) {
            val liveWorker: HashSet<String> = HashSet()
            for (worker in workerList) liveWorker.add(worker as String)

            val nameSuffix = slice.getString("name")
            val option = JsonObject()
            val dbcoll = query.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            option.put("db", dbcoll[0])
            option.put("skip", query.numberToSkip)
            option.put("limit", query.numberToReturn)
            option.put("query", JsonObject(query.query.toJson()))
            option.put("coll", dbcoll[1] + nameSuffix)

            val master = slice.getJsonObject("data").getString("master") // 从主节点中查找数据
            if (liveWorker.contains(master)) {
                val resp = awaitResult<Message<Buffer>> { h -> vertx.eventBus().send(master, option, h) }.body()
                return@async resp.bytes
            }

            val slave = slice.getJsonObject("data").getString("slave") // 从主节点中查找数据
            if (liveWorker.contains(slave)) {
                val resp = awaitResult<Message<Buffer>> { h -> vertx.eventBus().send(slave, option, h) }.body()
                return@async resp.bytes
            }

            return@async byteArrayOf(0, 0, 0, 0) // 4各字节全0代表数据长度为0

        }.await()
    }

    /**
     * 查找虚拟表
     **/
    suspend fun findPhony(query: Query, head: MongoHead, socket: NetSocket, additinal: Any): Boolean {
        return async(CommonPool) {
            val dbcoll = query.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (!ControlInfo.isPhony(dbcoll[0], dbcoll[1])) return@async false  // 非虚拟数据库, 直接退出
            val doc = ControlInfo.getPhonyDoc(dbcoll[0], dbcoll[1], additinal) as ControlInfo.DocInfo
            val reply = Reply()
            reply.documents = doc.data
            reply.numberReturned = doc.count // 返回数据条数
            reply.startingFrom = 0
            reply.cursorID = 0
            val aout = reply.pack(head.requestID + 10, head.requestID, 1) // 异步结果
            socket.write(Buffer.buffer().appendBytes(aout))

            return@async true

        }.await()
    }

    override fun handle(vertx: Vertx, head: MongoHead, body: MongoBody, socket: NetSocket) {
        launch(vertx.dispatcher()) {
            val reply = Reply()
            // 从zookeeper中查询在线worker列表
            var resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_WORKER_LIST", "", h) }.body()
            val workerList = resp.getJsonArray("data", JsonArray()) // ["127.0.0.1:27017","112.74.167.132:27017"]

            // master下线, 则读取slave节点, 若无slave，则返回空
            if (workerList.size() == 0) { // 当前无工作的节点, 显然无法插入数据
                reply.numberReturned = 0 // 返回数据条数
                reply.startingFrom = 0
                reply.cursorID = 0
                val aout = reply.pack(head.requestID + 10, head.requestID, 1) // 异步结果
                socket.write(Buffer.buffer().appendBytes(aout))
                logger.warn("no live worker!")
                return@launch
            }

            // 查询collection对应的slice所在节点
            val query = body as Query
            if (findPhony(query, head, socket, workerList)) return@launch // 虚拟数据库直接返回, 不查表
            val dbcoll = query.fullCollectionName.trim { it <= ' ' }.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // {"code":0,"data":[{"name":"0","data":{"count":16,"master":"127.0.0.1:27017"}}]}
            resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_SUB_COLLECTION_LIST", String.format("/%s/%s", dbcoll[0], dbcoll[1]), h) }.body()
            val defList = ArrayList<Deferred<*>>()
            for (json in resp.getJsonArray("data", JsonArray())) {
                var deferred = async { findAsync(vertx, query, workerList, json as JsonObject) } // 查询各mongo节点
                defList.add(deferred)
            }

            val retList = ArrayList<ByteArray>()
            var length = 0
            var size = 0
            for (deferred in defList) {
                val ret = deferred.await() as ByteArray// 某个子集合返回的数据
                val disMsg = DisMsg(ret)
                retList.add(ret)
                length = length + ret.size - 4 // 4个字节代表记录的条数信息
                size = size + disMsg.numberReturned
            }
            val out = ByteArray(length)

            var dstPos = 0
            for (i in retList.indices) {
                System.arraycopy(retList[i], 4, out, dstPos, retList[i].size - 4) // 把各节点的返回数据拷贝到out
                dstPos = dstPos + retList[i].size - 4
            }

            reply.documents = out
            reply.numberReturned = size // 返回数据条数
            reply.startingFrom = 0
            reply.cursorID = 0
            val aout = reply.pack(head.requestID + 10, head.requestID, 1) // 异步结果
            socket.write(Buffer.buffer().appendBytes(aout))

        }
    }
}