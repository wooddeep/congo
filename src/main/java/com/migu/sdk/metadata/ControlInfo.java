package com.migu.sdk.metadata;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.BSONObject;
import org.bson.BasicBSONEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lihan on 2018/8/31.
 */
public class ControlInfo {

    public static class DocInfo {
        public int count;
        public byte [] data;
    }

    public static JsonArray getInitDbList() {
        JsonArray initDbList = new JsonArray();
        initDbList.add("congo");
        return initDbList;
    }

    public static boolean isPhonyDb(String dbname) {
        return dbname.contains("congo");
    }

    public static JsonArray getPhonyColl(String dbname) {
        return new JsonArray("[{\"name\":\"online\",\"children\":[]}]");
    }

    public static boolean isPhony(String db, String coll) {
        return db.equals("congo");
    }

    public static DocInfo getPhonyDoc(String db, String coll, Object additional) {
        DocInfo out = new DocInfo();
        out.count = 0;
        out.data = new byte[0];
        if (db.equals("congo") && coll.equals("online")) {
            BasicBSONEncoder encoder = new BasicBSONEncoder();
            List<byte[]> list = new ArrayList<>();
            int length = 0;
            JsonArray workerList = (JsonArray)additional;
            for (int i = 0; i < workerList.size(); i++) {
                BSONObject bson = (BSONObject) com.mongodb.util.JSON.parse(
                    new JsonObject().put("key", workerList.getString(i)).toString());
                byte[] bsonByte = encoder.encode(bson);
                list.add(bsonByte);
                length += bsonByte.length;
            }

            byte [] bsonByte = new byte[length];
            int pos = 0;
            for (int i = 0; i < list.size(); i++) {
                System.arraycopy(list.get(i), 0, bsonByte, pos, list.get(i).length);
                pos += list.get(i).length;
            }

            out.count = workerList.size();
            out.data = bsonByte;
        }

        return out;
    }
}
