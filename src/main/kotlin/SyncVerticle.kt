/**
 * Created by lihan on 2018/7/25.
 */
package com.migu.tsg

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

// https://zhuanlan.zhihu.com/p/28046403
// https://juejin.im/entry/5b149f286fb9a01e7f2e96ee CoroutineVerticle

class SyncVerticle : AbstractVerticle() {

    // Called when verticle is deployed
    override fun start() {
        //val timerId = awaitEvent<Long> { handler -> vertx.setTimer(1000, handler) }
        //println("Event fired from timer with id ${timerId}")
        //awaitEvent<Long> { handler -> vertx.setPeriodic(1000, handler) }
        //println("b")

        //vertx.setPeriodic(1000, { h -> println("hello world") })

//        launch(vertx.dispatcher()) {
//            val timerId = awaitEvent<Long> { handler -> vertx.setTimer(1000, handler) }
//            println("Event fired from timer with id ${timerId}")
//
//            var resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_WORKER_LIST", "", h) }.body()
//            println("## worker list: ${resp}")
//
//            resp = awaitResult<Message<JsonObject>> { h -> vertx.eventBus().send("GET_CHILDREN_LIST", "", h) }.body()
//            println("## collection list: ${resp}")
//        }

    }

    // Optional - called when verticle is undeployed
    override fun stop() {
    }
}