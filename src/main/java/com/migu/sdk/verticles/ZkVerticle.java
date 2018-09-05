package com.migu.sdk.verticles;

import com.migu.sdk.metadata.ControlInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.HashMap;
import java.util.List;

import static com.migu.sdk.entry.Configure.sysConfig;

/**
 * Created by lihan on 2018/6/22.
 */

// curator async example:
// https://curator.apache.org/curator-x-async/async.html
// http://colobu.com/2014/12/16/zookeeper-recipes-by-example-8/
// https://www.jianshu.com/p/70151fc0ef5d
// Curator基础api
// https://www.hifreud.com/2017/01/12/zookeeper-09-java-api-curator-01-normal/

// zookeeper入门之Curator的使用之几种监听器的使用
// https://blog.csdn.net/sqh201030412/article/details/51446434

// Apache Curator学习笔记
// https://blog.gmem.cc/apache-curator-study-note

public class ZkVerticle extends AbstractVerticle {

    private ConcurrentHashSet<String> onlineWorker = new ConcurrentHashSet<>();

    private void getChildrenList(CuratorFramework client, String path, HashMap<String, JsonObject> out) throws Exception {
        List<String> children = client.getChildren().forPath(path);
        if (children.size() == 0) {
            String value = new String(client.getData().forPath(path));
            if (value.substring(0, 1).equals("{")) {
                out.put(path, new JsonObject(new String(client.getData().forPath(path))));
            }
        } else {
            for (String child : children) {
                getChildrenList(client, path + "/" + child, out);
            }
        }
    }

    @Override
    public void start() throws Exception {

        vertx.executeBlocking(future -> {
            // String zookeeperConnectionString = "172.21.0.1:2181,172.21.0.2:2181,172.21.0.3:2181"; // 多zk节点
            final String connectString = sysConfig.getString("zklist", "112.74.167.132:2181");
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3); // 重连策略
            CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, retryPolicy);
            client.start();
            future.complete(client);
        }, rs -> {
            CuratorFramework client = (CuratorFramework) rs.result();

            vertx.eventBus().<String>consumer("ADD_WORKER", h -> {
                try {
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).
                        inBackground((curatorFramework, curatorEvent) -> {
                            System.out.println(String.format("eventType:%s,resultCode:%s",
                                curatorEvent.getType(), curatorEvent.getResultCode()));

                            h.reply(new JsonObject().put("code", 0)); // ok
                        }).forPath("/worker" + h.body(), h.body().getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    h.reply(new JsonObject().put("code", 1)); // insert to zk fail!
                }
            });

            if (sysConfig.getString("master", "false").equals("true")) { // 主控节点侦测worker的上下线
                try {
                    TreeCache treeCache = new TreeCache(client, "/worker");
                    treeCache.getListenable().addListener((client1, event) -> {  //设置监听器和处理过程
                        ChildData data = event.getData();
                        if (data != null) {
                            switch (event.getType()) {
                                case NODE_ADDED: // WORKER节点加入
                                    String workerKey = new String(data.getData());
                                    if (workerKey == null) return;
                                    String[] keys = workerKey.replace("/", "").split(":");
                                    if (keys.length < 2) return;

                                    vertx.deployVerticle(new WorkerVerticle(), new DeploymentOptions()
                                        .setConfig(new JsonObject().put("host", keys[0]).put("port", Integer.parseInt(keys[1]))));

                                    // NODE_ADDED : /worker/127.0.0.1:27017  数据:/127.0.0.1:27017
                                    System.out.println("NODE_ADDED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    onlineWorker.add(workerKey.replace("/", ""));
                                    break;
                                case NODE_REMOVED:
                                    workerKey = new String(data.getData());
                                    onlineWorker.remove(workerKey.replace("/", ""));
                                    System.out.println("NODE_REMOVED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    break;
                                case NODE_UPDATED:
                                    System.out.println("NODE_UPDATED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    treeCache.start(); //开始监听
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!sysConfig.getString("master", "false").equals("true")) { // 工作节点侦测/db目录的变化
                try {
                    TreeCache treeCache = new TreeCache(client, "/db");
                    treeCache.getListenable().addListener((client1, event) -> {  //设置监听器和处理过程
                        ChildData data = event.getData();
                        if (data != null) {
                            switch (event.getType()) {
                                case NODE_ADDED:
                                    System.out.println("NODE_ADDED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    break;
                                case NODE_REMOVED:
                                    System.out.println("NODE_REMOVED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    break;
                                case NODE_UPDATED:
                                    System.out.println("NODE_UPDATED : " + data.getPath() + "  数据:" + new String(data.getData()));
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    treeCache.start(); //开始监听
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            vertx.eventBus().<String>consumer("GET_WORKER_LIST", h -> {
                vertx.executeBlocking(future -> {
                    try {
                        JsonArray data = new JsonArray();
                        onlineWorker.stream().forEach(str -> data.add(str.replace("/", ""))); // 去掉开始的 '/'
                        h.reply(new JsonObject().put("code", 0).put("data", data)); // ok
                        future.complete();
                    } catch (Exception e) {
                        e.printStackTrace();
                        h.reply(new JsonObject().put("code", 1).put("msg", e.getMessage())); // insert to zk fail!
                        future.failed();
                    }
                }, irs -> {

                });
            });

            vertx.eventBus().<String>consumer("ADD_DATABASE", h -> {
                try {
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).
                        inBackground((curatorFramework, curatorEvent) -> {
                            System.out.println(String.format("eventType:%s,resultCode:%s",
                                curatorEvent.getType(), curatorEvent.getResultCode()));
                            h.reply(new JsonObject().put("code", 0)); // ok
                        }).forPath("/db" + h.body());
                } catch (Exception e) {
                    e.printStackTrace();
                    h.reply(new JsonObject().put("code", 1).put("msg", e.getMessage())); // insert to zk fail!
                }
            });

            vertx.eventBus().<String>consumer("GET_DATABASE_LIST", h -> {
                vertx.executeBlocking(future -> {
                    try {
                        List<String> children = client.getChildren().forPath("/db");
                        JsonArray data = ControlInfo.getInitDbList();
                        children.stream().forEach(str -> data.add(str));
                        h.reply(new JsonObject().put("code", 0).put("data", data)); // ok
                        future.complete();
                    } catch (Exception e) {
                        e.printStackTrace();
                        h.reply(new JsonObject().put("code", 1).put("msg", e.getMessage())); // insert to zk fail!
                        future.failed();
                    }
                }, irs -> {

                });
            });

            vertx.eventBus().<String>consumer("ADD_COLLECTION", h -> {
                try {
                    System.out.println(h.body());
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).
                        inBackground((curatorFramework, curatorEvent) -> {
                            System.out.println(String.format("eventType:%s,resultCode:%s",
                                curatorEvent.getType(), curatorEvent.getResultCode()));
                            System.out.println(curatorEvent.getPath());
                            h.reply(new JsonObject().put("code", 0)); // ok
                        }).forPath("/db" + h.body() + "/0", new JsonObject() // 集合分片
                        .put("count", 0).put("master", "127.0.0.1:27017").toString().getBytes()); // TODO 替换master的ip:port


                } catch (Exception e) {
                    e.printStackTrace();
                    h.reply(new JsonObject().put("code", 1)); // insert to zk fail!
                }
            });

            // 添加集合分片
            vertx.eventBus().<JsonObject>consumer("ADD_COLLECTION_SLICE", h -> {
                try {
                    System.out.println(h.body());
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).
                        inBackground((curatorFramework, curatorEvent) -> {
                            System.out.println(String.format("eventType:%s,resultCode:%s",
                                curatorEvent.getType(), curatorEvent.getResultCode()));
                            System.out.println(curatorEvent.getPath());
                            h.reply(new JsonObject().put("code", 0)); // ok
                        }).forPath("/db" + h.body().getString("path"),
                        h.body().getJsonObject("data").toString().getBytes()); // TODO 替换master的ip:port

                } catch (Exception e) {
                    e.printStackTrace();
                    h.reply(new JsonObject().put("code", 1)); // insert to zk fail!
                }
            });

            // 添加集合分片条数增加
            vertx.eventBus().<String>consumer("INC_COLLECTION_SLICE", h -> {
                vertx.executeBlocking(future -> {
                    try {
                        byte[] data = client.getData().forPath("/db" + h.body());
                        JsonObject json = new JsonObject(new String(data));
                        json.put("count", json.getInteger("count", 0) + 1);
                        client.setData().forPath("/db" + h.body(), json.toString().getBytes()); // 回写
                        h.reply(new JsonObject().put("code", 0)); // ok
                    } catch (Exception e) {
                        e.printStackTrace();
                        h.reply(new JsonObject().put("code", 1)); // insert to zk fail!
                    }
                }, irs -> {
                    // TODO
                });
            });

            vertx.eventBus().<String>consumer("GET_COLLECTION_LIST", h -> {
                vertx.executeBlocking(future -> {
                    try {
                        String childPath = "/db" + h.body();
                        System.out.printf("## db: %s\n", h.body());
                        List<String> children = client.getChildren().forPath(childPath);
                        JsonArray data = new JsonArray();
                        for (String str : children) { // collection
                            JsonObject coll = new JsonObject();
                            coll.put("name", str);
                            coll.put("children", new JsonArray());
                            String grandsonPath = childPath + "/" + str;
                            List<String> grandson = client.getChildren().forPath(grandsonPath);
                            for (String gs : grandson) {
                                JsonObject child = new JsonObject();
                                child.put("name", gs);
                                JsonObject nodeData =
                                    new JsonObject(new String(client.getData().forPath(grandsonPath + "/" + gs)));
                                child.put("data", nodeData);
                                coll.getJsonArray("children").add(child);
                            }
                            data.add(coll);
                        }

                        h.reply(new JsonObject().put("code", 0).put("data", data)); // ok
                        future.complete();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        if (ControlInfo.isPhonyDb(h.body())) { // 判断是否虚拟数据库
                            h.reply(new JsonObject().put("code", 0).put("data", ControlInfo.getPhonyColl(h.body())));
                        } else {
                            h.reply(new JsonObject().put("code", 1).put("msg", "no collection find!")); // insert to zk fail!
                        }

                        future.failed();
                    }
                }, irs -> {

                });
            });

            vertx.eventBus().<String>consumer("GET_SUB_COLLECTION_LIST", h -> {
                vertx.executeBlocking(future -> {
                    try {
                        String childPath = "/db" + h.body();
                        List<String> children = client.getChildren().forPath(childPath);
                        JsonArray data = new JsonArray();
                        for (String gs : children) {
                            JsonObject child = new JsonObject();
                            child.put("name", gs);
                            JsonObject nodeData =
                                new JsonObject(new String(client.getData().forPath(childPath + "/" + gs)));
                            System.out.println(nodeData);
                            child.put("data", nodeData);
                            data.add(child);
                        }

                        h.reply(new JsonObject().put("code", 0).put("data", data)); // ok
                        future.complete();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        h.reply(new JsonObject().put("code", 1)
                            .put("msg", "no collection find!").put("data", new JsonArray())); // insert to zk fail!
                        future.failed();
                    }
                }, irs -> {

                });
            });

            vertx.eventBus().<String>consumer("GET_CHILDREN_LIST", h -> {
                vertx.executeBlocking(future -> {
                    try {
                        HashMap<String, JsonObject> childMap = new HashMap<>();
                        getChildrenList(client, "/db", childMap);

                        JsonArray out = new JsonArray();
                        childMap.forEach((k, v) -> {
                            JsonObject child = new JsonObject();
                            child.put("node", v);
                            out.add(child);
                        });

                        h.reply(new JsonObject().put("code", 0).put("data", out)); // ok
                        future.complete();
                    } catch (Exception e) {
                        e.printStackTrace();
                        h.reply(new JsonObject().put("code", 1).put("msg", e.getMessage())); // insert to zk fail!
                        future.failed();
                    }
                }, irs -> {

                });
            });

        });
    }
}
