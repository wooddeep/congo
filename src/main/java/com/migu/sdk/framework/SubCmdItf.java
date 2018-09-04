package com.migu.sdk.framework;

import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * Created by lihan on 2018/6/27.
 */

@FunctionalInterface
public interface SubCmdItf {
    void response(Vertx vertx, MongoBody query, MongoHead head, NetSocket socket);
}
