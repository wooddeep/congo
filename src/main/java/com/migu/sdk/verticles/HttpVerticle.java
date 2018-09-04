package com.migu.sdk.verticles;

import com.migu.sdk.constant.KeyDefine;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.migu.sdk.entry.CmdLine.cmlConfig;
import static com.migu.sdk.entry.Configure.sysConfig;

/**
 * Created by lihan on 2017/11/14.
 */
public class HttpVerticle extends AbstractVerticle {

    private Logger logger = LogManager.getLogger("frame");

    @Override
    public void start() throws Exception {

        ClusterManager clusterManager = ((VertxInternal) vertx).getClusterManager();
        HazelcastClusterManager manager = (HazelcastClusterManager) clusterManager;

        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST).allowedMethod(HttpMethod.OPTIONS)
            .allowedHeader("Content-Type"));
        router.route().handler(BodyHandler.create());

        // 添加数据库(固定)
        // curl -X PUT http://127.0.0.1:9090/add/db/sdk
        // 添加成功：{"action":"set","node":{"key":"/database/test","dir":true,"modifiedIndex":17791,"createdIndex":17791}}
        // 已经存在: {"errorCode":102,"message":"Not a file","cause":"/database/test","index":17789}
        // 删除 curl http://127.0.0.1:2379/v2/keys/database?recursive=true -XDELETE
        router.route("/add/db/:dbname").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            String dbname = routingContext.request().getParam("dbname");
            vertx.eventBus().<JsonObject>send("ADD_DATABASE", "/" + dbname, h -> {
                if (h.succeeded()) {
                    response.end(h.result().body().toString());
                } else {
                    response.end(new JsonObject().put("code", -1).put("msg", "please wait!").toString());
                }
            });
        });

        // 查询数据库
        // curl -X GET http://127.0.0.1:9090/get/db
        // curl -X GET http://localhost:2379/v2/keys/works
        // {"action":"get","node":{"key":"/works","dir":true,"nodes":[{"key":"/works/127.0.0.1:27017","value":"","modifiedIndex":17781,"createdIndex":17781}],"modifiedIndex":17779,"createdIndex":17779}}
        router.route("/get/db").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            vertx.eventBus().<JsonObject>send("GET_DATABASE_LIST", "", h -> {
                if (h.succeeded()) {
                    response.end(h.result().body().toString());
                } else {
                    response.end(new JsonObject().put("code", -1).put("msg", "please wait!").toString());
                }
            });
        });

        // 添加集合
        // curl -X GET http://127.0.0.1:9090/add/coll/sdk/alarm
        router.route("/add/coll/:dbname/:collname").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            String dbname = routingContext.request().getParam("dbname");
            String collname = routingContext.request().getParam("collname");

            vertx.eventBus().<JsonObject>send("ADD_COLLECTION", "/" + dbname + "/" + collname, h -> {
                if (h.succeeded()) {
                    response.end(h.result().body().toString());
                } else {
                    response.end(new JsonObject().put("code", -1).put("msg", "please wait!").toString());
                }
            });
        });

        // 查询集合
        // curl -X GET http://127.0.0.1:9090/get/coll/sdk
        router.route("/get/coll/:dbname").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            String dbname = routingContext.request().getParam("dbname");

            vertx.eventBus().<JsonObject>send("GET_COLLECTION_LIST", "/" + dbname, h -> {
                if (h.succeeded()) {
                    response.end(h.result().body().toString());
                } else {
                    response.end(new JsonObject().put("code", -1).put("msg", "please wait!").toString());
                }
            });
        });

        // 添加worker节点(零时)
        // curl -X GET http://127.0.0.1:9090/add/worker/127.0.0.1:27017
        router.route("/add/worker/:worker").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            String worker = routingContext.request().getParam("worker");
            vertx.eventBus().<JsonObject>send("ADD_WORKER", "/" + worker, h -> {
                if (h.succeeded()) {
                    response.end(h.result().body().toString());
                } else {
                    response.end(new JsonObject().put("code", -1).put("msg", "please wait!").toString());
                }
            });

        });

        server.requestHandler(router::accept).listen(sysConfig.getInteger("port"), h -> {
            Integer port = h.result().actualPort();
            if (port == null) {
                logger.info("## start server fail! maybe the port number is invalid!");
                System.exit(-1);
            }
            if (!sysConfig.getBoolean("standalone", true)) { // cluster mode!
                String host = sysConfig.getString("clusterHost");
                String nodeId = manager.getNodeID();
                manager.getAsyncMap(KeyDefine.CLUSTER_MAP_NAME, ar -> {
                    if (ar.succeeded()) {
                        String nodeInfo = host + ":" + port; // 以nodeId为键值
                        cmlConfig.put("nodeInfo", nodeInfo);
                        ar.result().put(nodeId, cmlConfig, (x) -> {
                            logger.info("## node info : " + nodeInfo);
                        });
                    }
                });
            }
        });
    }
}
