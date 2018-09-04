package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.tsg.SyncInsert;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * Created by lihan on 2018/6/27.
 */
public class InsertDoc {

    @SubCommand(name = "insert")
    public static SubCmdItf insert = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        SyncInsert syncOper = new SyncInsert();
        syncOper.handle(vertx, head, body, socket);
    };
}
