package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.tsg.SyncFind;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * Created by lihan on 2018/6/27.
 */
public class Find {

    @SubCommand(name = "find")
    public static SubCmdItf find = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        SyncFind syncFind = new SyncFind();
        syncFind.handle(vertx, head, body, socket);
    };

}
