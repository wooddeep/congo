package com.migu.sdk.verticles;

import com.migu.sdk.protocol.DisMsg;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.operation.UpdateOperation;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.bson.BSONObject;
import org.bson.BasicBSONEncoder;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lihan on 2018/6/19.
 */


//To convert a string json to bson, do:
//
//import org.bson.BasicBSONEncoder;
//import org.bson.BSONObject;
//
//BSONObject bson = (BSONObject)com.mongodb.util.JSON.parse(string_json);
//    BasicBSONEncoder encoder = new BasicBSONEncoder();
//    byte[] bson_byte = encoder.encode(bson);
//
//To convert a bson to json, do:
//
//import org.bson.BasicBSONDecoder;
//import org.bson.BSONObject;
//
//BasicBSONDecoder decoder = new BasicBSONDecoder();
//    BSONObject bsonObject = decoder.readObject(out);
//    String json_string = bsonObject.toString();

// java -jar target\congo-0.0.1-SNAPSHOT.jar -W true -c ../conf -x 127.0.0.1:27017

public class WorkerVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {

        // basic data
        JsonObject config = config();
        String host = config.getString("host", "127.0.0.1");
        int port = config.getInteger("port", 27017);
        BasicBSONEncoder encoder = new BasicBSONEncoder();

        // mongo-driver
        com.mongodb.MongoClient mongoClient = new com.mongodb.MongoClient(host, port);

        vertx.eventBus().<JsonObject>consumer(host + ":" + port, ar -> {
            JsonObject option = ar.body();

            vertx.executeBlocking(future -> {
                MongoDatabase db = mongoClient.getDatabase(option.getString("db", "test")); // TODO replace the database name
                MongoCollection<Document> collection = db.getCollection(option.getString("coll", "test"));
                FindIterable<Document> list = collection.find()
                    .limit(option.getInteger("limit", -1))
                    .sort(BasicDBObject.parse(option.getJsonObject("sort", new JsonObject()).toString()))
                    .skip(0);

                List<byte[]> byteArrList = new ArrayList<>();
                int len = 0;
                int size = 0;

                for (Document doc : list) {
                    BSONObject bson = (BSONObject) com.mongodb.util.JSON.parse(doc.toJson()); // TODO 修改
                    byte[] byteArr = encoder.encode(bson);
                    byteArrList.add(byteArr);
                    len = len + byteArr.length;
                    size++;
                }

                DisMsg disMsg = new DisMsg(size);
                byte[] out = new byte[disMsg.getBytes().length + len];
                System.arraycopy(disMsg.getBytes(), 0, out, 0, disMsg.getBytes().length);
                int offset = disMsg.getBytes().length;
                for (byte[] arr : byteArrList) {
                    System.arraycopy(arr, 0, out, offset, arr.length);
                    offset = offset + arr.length;
                }
                ar.reply(Buffer.buffer(out));

            }, iar -> {
            });

        });

        // 插入数据
        vertx.eventBus().<JsonObject>consumer(host + ":" + port + ":insert", ar -> {
            JsonObject option = ar.body();
            vertx.executeBlocking(future -> {
                MongoDatabase db = mongoClient.getDatabase(option.getString("db", "test"));
                MongoCollection<Document> collection = db.getCollection(option.getString("coll", "test"));
                collection.insertOne(Document.parse(option.getString("data", "{}").toString()));
                ar.reply(new JsonObject().put("code", 0));
            }, iar -> {
            });
        });

        // update数据
        // https://blog.csdn.net/zpf336/article/details/50763987 -- mongo update 操作
        vertx.eventBus().<JsonObject>consumer(host + ":" + port + ":update", ar -> {
            JsonObject option = ar.body();
            vertx.executeBlocking(future -> {
                MongoDatabase db = mongoClient.getDatabase(option.getString("db", "test"));
                MongoCollection<Document> collection = db.getCollection(option.getString("coll", "test"));
                Document filter = Document.parse(option.getString("query"));

                //注意update文档里要包含"$set"字段
                Document update = new Document();
                update.append("$set", Document.parse(option.getString("update")));
                UpdateResult result = collection.updateOne(filter, update);

                ar.reply(result.getModifiedCount());
            }, iar -> {
            });
        });

        // upsert == true && multi == false
        vertx.eventBus().<JsonObject>consumer(host + ":" + port + ":update:true", ar -> {
            JsonObject option = ar.body();
            vertx.executeBlocking(future -> {
                MongoDatabase db = mongoClient.getDatabase(option.getString("db", "test"));
                MongoCollection<Document> collection = db.getCollection(option.getString("coll", "test"));
                Document filter = Document.parse(option.getString("query"));

                //注意update文档里要包含"$set"字段
                Document update = new Document();
                update.append("$set", Document.parse(option.getString("update")));
                UpdateResult result = collection.updateOne(filter, update);

                ar.reply(result.getModifiedCount());
            }, iar -> {
            });
        });

        // upsert == false && multi == true
        vertx.eventBus().<JsonObject>consumer(host + ":" + port + ":update:false:true", ar -> {
            JsonObject option = ar.body();
            vertx.executeBlocking(future -> {
                MongoDatabase db = mongoClient.getDatabase(option.getString("db", "test"));
                MongoCollection<Document> collection = db.getCollection(option.getString("coll", "test"));
                Document filter = Document.parse(option.getString("query"));

                //注意update文档里要包含"$set"字段
                Document update = new Document();
                update.append("$set", Document.parse(option.getString("update")));
                UpdateResult result = collection.updateMany(filter, update);
                ar.reply(result.getModifiedCount());
            }, iar -> {
            });
        });

        vertx.eventBus().<JsonObject>consumer(host + ":" + port + ":delete", ar -> {
            JsonObject option = ar.body();
            vertx.executeBlocking(future -> {
                MongoDatabase db = mongoClient.getDatabase(option.getString("db", "test"));
                MongoCollection<Document> collection = db.getCollection(option.getString("coll", "test"));

                DeleteResult result = collection.deleteMany(Document.parse(option.getString("query")));
                ar.reply(result.getDeletedCount());
            }, iar -> {
            });
        });
    }


}
