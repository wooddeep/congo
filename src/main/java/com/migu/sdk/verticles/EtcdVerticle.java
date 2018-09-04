package com.migu.sdk.verticles;

import io.vertx.core.AbstractVerticle;

/**
 * Created by lihan on 2018/6/22.
 */
public class EtcdVerticle extends AbstractVerticle {


    @Override
    public void start() throws Exception {

        vertx.eventBus().consumer("ADD_WORKER", h -> {

        });

        //PutResponse result = kvClient.put(ByteString.copyFrom("key".getBytes()), ByteString.copyFrom("value".getBytes())).sync();

        //RangeResponse result1 = kvClient.get(ByteString.copyFrom("key".getBytes())).asPrefix().sync();

        //List<KeyValue> list = result1.getKvsList();

        //System.out.println(list.get(0));
    }
}
