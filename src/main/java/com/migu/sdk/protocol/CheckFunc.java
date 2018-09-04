package com.migu.sdk.protocol;

/**
 * Created by lihan on 2018/2/9.
 */

import io.vertx.core.json.JsonObject;

/**
 * https://www.cnblogs.com/runningTurtle/p/7092632.html
 **/

@FunctionalInterface
interface CheckFunc {
    boolean handle(JsonObject param);
}

