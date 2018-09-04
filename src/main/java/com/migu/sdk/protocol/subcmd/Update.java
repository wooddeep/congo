package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.tsg.SyncUpdate;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * Created by lihan on 2018/7/9.
 */

public class Update {
    @SubCommand(name = "update")
    public static SubCmdItf update = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        SyncUpdate syncOper = new SyncUpdate();
        syncOper.handle(vertx, head, body, socket);
    };
}
