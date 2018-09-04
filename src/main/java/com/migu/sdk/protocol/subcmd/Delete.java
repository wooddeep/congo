package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.tsg.SyncDelete;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * Created by lihan on 2018/7/9.
 */
public class Delete {
    @SubCommand(name = "delete")
    public static SubCmdItf delete = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        SyncDelete syncOper = new SyncDelete();
        syncOper.handle(vertx, head, body, socket);
    };
}
