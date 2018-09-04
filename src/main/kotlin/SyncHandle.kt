/**
 * Created by lihan on 2018/7/13.
 */

package com.migu.tsg

import com.migu.sdk.protocol.MongoBody
import com.migu.sdk.protocol.MongoHead
import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket

open class SyncHandle {
    open fun handle(vertx: Vertx, head: MongoHead, body: MongoBody, socket: NetSocket) {}
}