package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.framework.annotate.SubCommand;
import com.migu.sdk.protocol.MongoBody;
import com.migu.sdk.protocol.MongoHead;
import com.migu.sdk.protocol.Query;
import com.migu.sdk.protocol.Reply;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import org.bson.BSON;
import org.bson.BasicBSONObject;
import org.bson.RawBsonDocument;
import org.bson.types.BasicBSONList;

/**
 * Created by lihan on 2018/6/27.
 */
public class ListCollections {

    @SubCommand(name = "listCollections")
    public static SubCmdItf listCollections = (Vertx vertx, MongoBody body, MongoHead head, NetSocket socket) -> {
        Query query = (Query)body;
        String dbname = query.fullCollectionName.split("\\.\\$")[0];
        vertx.eventBus().<JsonObject>send("GET_COLLECTION_LIST", "/" + dbname, h -> {

            BasicBSONList collList = new BasicBSONList();
            BasicBSONObject object = new BasicBSONObject();
            object.put("ok", 1);
            Reply reply = new Reply();

            if (h.succeeded()) {
                JsonObject resp = h.result().body();
                if (resp.getInteger("code", 1) == 1) {
                    BasicBSONObject cursor = new BasicBSONObject();
                    cursor.put("id", 0l);
                    cursor.put("ns", "sdk.$cmd.listCollections");
                    cursor.put("firstBatch", collList);
                    object.put("cursor", cursor);
                    object.put("totalSize", 0);
                } else {
                    resp.getJsonArray("data").forEach(coll -> {
                        BasicBSONObject coll0 = new BasicBSONObject();
                        coll0.put("name", ((JsonObject)coll).getString("name"));
                        coll0.put("options", new BasicBSONObject());
                        collList.add(coll0);
                    });

                    BasicBSONObject cursor = new BasicBSONObject();
                    cursor.put("id", 0l);
                    cursor.put("ns", "sdk.$cmd.listCollections");
                    cursor.put("firstBatch", collList);
                    object.put("cursor", cursor);
                    object.put("totalSize", 32768);
                }

                reply.documents = new RawBsonDocument(BSON.encode(object));
                byte[] out = reply.pack(head.requestID + 10, head.requestID, 1);
                socket.write(Buffer.buffer().appendBytes(out));

            } else {

            }
        });
    };
}
