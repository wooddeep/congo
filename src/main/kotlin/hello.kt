import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.*
import kotlin.system.measureTimeMillis

// 次时代Java编程(二) vertx-lang-kotlin-coroutines介绍
// http://www.streamis.me/2017/09/29/java-next-generation-2/

// 协程理解 *
// https://blog.csdn.net/BeyondWorlds/article/details/79866611

/*
fun main(args: Array<String>) {
    launch(CommonPool) {
        delay(1000L) // 延迟1S
        //print("World0!,") // 打印World
        //print(Thread.currentThread().getName())
    }

    launch(CommonPool) {
        delay(1000L) // 延迟1S
        //print("World1!,") // 打印World
        //print(Thread.currentThread().getName())
    }

    //print("Hello,") // 在launch块外面则是Main线程
    //print(Thread.currentThread().getName())
    Thread.sleep(2000L) // 主线程休眠2S

    val json = JsonObject()
    var b = json.getJsonObject("abc") as JsonObject?
    //println(b)

    print(job0.isCompleted)
}
*/

fun main(args: Array<String>) {
    println("1:${System.currentTimeMillis()}")
    launch(CommonPool) {
        // 任务1会立即启动, 并且会在别的线程上并行执行
        val deferred1 = async { requestDataAsync1() }

        // 上一个步骤只是启动了任务1, 并不会挂起当前协程
        // 所以任务2也会立即启动, 也会在别的线程上并行执行
        val deferred2 = async { requestDataAsync3() }

        // 先等待任务1结束(等了约1000ms),
        // 然后等待任务2, 由于它和任务1几乎同时启动的, 所以也很快完成了
        println("2:${System.currentTimeMillis()}")
        println("x:${System.currentTimeMillis()}, data1=${deferred1.await()}, data2=${deferred2.await()}")
        println("3:${System.currentTimeMillis()}")
    }
    println("4:${System.currentTimeMillis()}")
    Thread.sleep(10000L) // 继续无视这个sleep
    println("5:${System.currentTimeMillis()}")
}

suspend fun requestDataAsync1(): String {
    delay(1000L)
    return "data1"
}

suspend fun requestDataAsync2(): String {
    delay(1000L)
    return "data2"
}

suspend fun requestDataAsync3(): String {
    return async(CommonPool) {
        // do something need lots of times.
        // ...
        "hello world"  // return data
    }.await()
}

/*
fun main(args: Array<String>) = runBlocking<Unit> {
    val time = measureTimeMillis {
        val one = async(CommonPool) { doSomethingUsefulOne() }
        val two = async(CommonPool) { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")

    val vertx = Vertx.vertx()
    launch(vertx.dispatcher()) {
        awaitEvent<Long> { handler ->
            vertx.setTimer(1000, handler)
        }
        println("Event fired from timer")
    }

}

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L)
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L)
    return 29
}
*/