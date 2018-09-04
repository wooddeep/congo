/**
 * Created by lihan on 2018/7/6.
 */

package com.migu.tsg

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

import java.lang.*

class KotlinDemo {
    var name: String = "hello world"

    fun demo(vertx: Vertx) {

        launch(vertx.dispatcher()) {

            println(System.currentTimeMillis()) // kotlin call java

            val timerId = awaitEvent<Long> { handler ->
                vertx.setTimer(1000, handler)
            }



            println("Event fired from timer with id ${timerId}")
            println(System.currentTimeMillis())
        }
    }

}
