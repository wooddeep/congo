package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.sdk.protocol.Query;
import com.migu.sdk.protocol.Reply;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.bson.BSON;
import org.bson.BasicBSONObject;
import org.bson.BsonValue;
import org.bson.RawBsonDocument;
import org.bson.types.BasicBSONList;

/**
 * Created by lihan on 2018/6/27.
 */
public class ListDatabases {

    @SubCommand(name = "listDatabases")
    public static SubCmdItf listDatabases = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        Query query = (Query)body;
        BasicBSONObject object = new BasicBSONObject();
        String subCmd = query.query.getFirstKey();
        BsonValue value = query.query.get(subCmd);
        Reply reply = new Reply();
        object.put("ok", 1);

        vertx.eventBus().<JsonObject>send("GET_DATABASE_LIST", "", h -> {
            BasicBSONList database = new BasicBSONList();
            if (h.succeeded()) {
                System.out.println(h.result().body());
                JsonArray dbs = h.result().body().getJsonArray("data");
                dbs.stream().forEach(dn -> {
                    BasicBSONObject db = new BasicBSONObject();
                    db.put("name", dn.toString());
                    db.put("sizeOnDisk", "32768"); // TODO
                    db.put("empty", false);
                    database.add(db);
                });
            }

            object.put("databases", database);
            object.put("totalSize", 32768);
            reply.documents = new RawBsonDocument(BSON.encode(object));
            byte[] aout = reply.pack(head.requestID + 10, head.requestID, 1);
            socket.write(Buffer.buffer().appendBytes(aout));

        });
    };
}
