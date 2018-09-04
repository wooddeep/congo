package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.sdk.protocol.Query;
import com.migu.sdk.protocol.Reply;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.bson.*;

/**
 * Created by lihan on 2018/6/27.
 */
public class GetLog {

    @SubCommand(name = "getLog")
    public static SubCmdItf getLog = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        //String dbname = query.fullCollectionName.split("\\.\\$")[0];
        Query query = (Query)body;
        BasicBSONObject object = new BasicBSONObject();
        String subCmd = query.query.getFirstKey();
        BsonValue value = query.query.get(subCmd);
        Reply reply = new Reply();
        object.put("ok", 1);
        object.put("log", new Document[0]);

        String sval = value.asString().getValue();

        reply.documents = new RawBsonDocument(BSON.encode(object));
        byte[] out = reply.pack(head.requestID + 10, head.requestID, 1);
        socket.write(Buffer.buffer().appendBytes(out));

    };
}
