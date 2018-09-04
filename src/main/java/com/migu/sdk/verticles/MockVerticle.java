package com.migu.sdk.verticles;

import com.migu.sdk.constant.MesgAddr;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Created by lihan on 2017/11/17.
 */
public class MockVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        //System.out.println("# deploymentId<1> = " + context.deploymentID());
        //String address =  vertx.container.config().getString("address");

        vertx.setPeriodic(20000000, h -> {
            //System.out.println("==================");
            JsonObject json = new JsonObject().put("evt", "0003");
            JsonArray odlt = new JsonArray("[{\"pcc\":\"80000000000100005060\"}]");
            json.put("req", new JsonObject().put("ctp", "1").put("odlt", odlt));

            json.put("resp", new JsonObject().put("rlt", "0"));
            vertx.eventBus().send(MesgAddr.ACT_LOG_REALTIME_APP, json);
            //vertx.eventBus().send("GET.TODAY.INDEX", ".", ih -> {
                //System.out.println("## resp:" + ih.result().body());
            //});

            vertx.undeploy(context.deploymentID());

        });

        vertx.eventBus().<JsonObject>consumer("MESG_ADDR", h -> {
            JsonObject json = h.body();
            // TODO
        });

    }
}
