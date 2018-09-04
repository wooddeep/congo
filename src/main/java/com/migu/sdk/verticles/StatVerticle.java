package com.migu.sdk.verticles;

import com.migu.sdk.constant.ChaosDefine;
import com.migu.sdk.constant.KeyDefine;
import com.migu.sdk.constant.MesgAddr;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by lihan on 2018/3/5.
 */
public class StatVerticle extends AbstractVerticle {
    private static Logger logger = LogManager.getLogger("stat");

    JsonObject stats = new JsonObject();

    @Override
    public void start() throws Exception {
        super.start();

        vertx.eventBus().consumer(MesgAddr.GET_TPS, h -> {
            h.reply(stats.getInteger(KeyDefine.TPS_REC, 0));
        });

        vertx.eventBus().<Integer>consumer(MesgAddr.SET_TPS, h -> {
            stats.put(KeyDefine.TPS_REC, stats.getInteger(KeyDefine.TPS_REC, 0) + 1);
        });

        /**
         * 周期定时, 统计每100秒钟的TPS
         **/
        vertx.setPeriodic(ChaosDefine.ROOT_PERODIC * 100, h -> {
            logger.info(stats.getInteger(KeyDefine.TPS_REC, 0));
            stats.put(KeyDefine.TPS_REC, 0);
        });

    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
