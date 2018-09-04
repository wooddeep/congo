package com.migu.sdk.protocol;

import com.migu.sdk.toolkit.NumberUtil;
import io.vertx.core.json.JsonObject;

import java.io.Serializable;

/**
 * Created by lihan on 2018/6/15.
 */
public class MongoHead implements Serializable {
    public int messageLength;
    public int requestID;
    public int responseTo;
    public int opCode;

    public void parse(byte [] arr) {
        this.messageLength = NumberUtil.byte4ToIntLittle(arr, 0);
        this.requestID = NumberUtil.byte4ToIntLittle(arr, 4);
        this.responseTo = NumberUtil.byte4ToIntLittle(arr, 8);
        this.opCode = NumberUtil.byte4ToIntLittle(arr, 12);
    }

    @Override
    public String toString() {
        JsonObject out = new JsonObject();
        out.put("messageLength", this.messageLength);
        out.put("requestID", this.requestID);
        out.put("responseTo", this.responseTo);
        out.put("opCode", this.opCode);
        return out.toString();
    }
}
