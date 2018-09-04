package com.migu.sdk.cluster;

import com.hazelcast.core.MembershipEvent;
import com.migu.sdk.constant.KeyDefine;
import com.migu.sdk.toolkit.LogUtil;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lihan on 2018/2/1.
 */
public class ClusterListener extends HazelcastClusterManager {

    private Logger logger = LogManager.getLogger("frame");
    private HazelcastClusterManager manager;
    private Vertx vertx;

    @Deprecated
    private void executeVbs() throws Exception {
        String tempFilePath = "G:\\";
        File tempDir = new File(tempFilePath);

        // 创建临时文件
        File temp = File.createTempFile("run", ".vbs", tempDir);
        System.out.println(temp.getAbsoluteFile());
        //在程序退出时删除临时文件
        temp.deleteOnExit();
        // 向临时文件中写入内容
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write("Set ws = CreateObject(\"Wscript.Shell\") \n" +
            "ws.run \"cmd /c java -jar F:\\work\\java\\ideal-ws\\sdk-mipay\\target\\" +
            "sdk-mipay-0.0.1-SNAPSHOT.jar -C 127.0.0.1 -L 127.0.0.1\", vbhide ");
        out.close();

        String[] cpCmd = new String[]{"wscript", temp.getAbsoluteFile().toString()};
        Process process = Runtime.getRuntime().exec(cpCmd);
        int val = process.waitFor();
        out.close();
    }


    public static void executeRestartCommand(JsonObject json) {
        try {
            // nodeInfo是节点注册时写入的, 非命令行参数, 需要过滤掉
            // 生成命令行参数
            String cml = json.stream().filter(obj -> !obj.getKey().equals("nodeInfo"))
                .map(obj -> "--" + obj.getKey() + " " + obj.getValue()).reduce((x, y) -> x + " " + y).get();

            Runtime.getRuntime().exec(String.format("java -jar %s %s",
                "F:\\work\\java\\ideal-ws\\sdk-mipay\\target\\sdk-mipay-0.0.1-SNAPSHOT.jar ",
                cml));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restart(AsyncMap<Object, Object> map, List<String> nodeList, JsonObject exitNode) {
        List<Future> flist = nodeList.stream().map(nodeId -> {
            Future<JsonObject> future = Future.future();
            map.get(nodeId, obj -> {
                future.complete((JsonObject) obj.result());
            });
            return future;
        }).collect(Collectors.toList());

        CompositeFuture.all(flist).setHandler(far -> {
            if (far.succeeded()) {
                String nodeAddr = exitNode.getString("nodeInfo").split(":")[0];

                int conflict = flist.stream()
                    .filter(f -> (JsonObject) f.result() != null) // 去除null对象
                    .map(f -> (JsonObject) f.result()) // 转换为 JsonObject流
                    .map(json -> { // 端口冲突检查
                            if (json.getString("nodeInfo").contains(nodeAddr)) { // 在同一主机之上
                                String existPort = json.getString("port");
                                String existTelPort = json.getString("telport");
                                String newPort = exitNode.getString("port");
                                String newTelPort = exitNode.getString("telport");
                                if (existPort.equals(newPort) || existTelPort.equals(newTelPort)) { // 端口冲突
                                    logger.info("## port confilit restart don't permit!");
                                    return 1;
                                }
                            }
                            return 0;
                        }
                    ).reduce((x, y) -> x + y).get();

                if (conflict == 0) {
                    logger.info("## I will restart the down node!!");
                    executeRestartCommand(exitNode);
                }
            }
        });
    }

    public ClusterListener(HazelcastClusterManager manager, Vertx vertx) {
        this.manager = manager;
        this.vertx = vertx;
    }

    @Override
    public synchronized void memberAdded(MembershipEvent membershipEvent) {
        System.out.println(String.format("Member Added: %s - %s ",
            membershipEvent.getMember().getUuid(), membershipEvent.getMember().toString()));
        super.memberAdded(membershipEvent);
    }

    @Override
    public synchronized void memberRemoved(MembershipEvent membershipEvent) {
        // yes I am manager, and the node Member [192.168.141.197]:5702
        membershipEvent.getMember().getUuid();
        System.out.println(String.format("Member Removed: %s - %s ",
            membershipEvent.getMember().getUuid(), membershipEvent.getMember().toString()));

        super.memberRemoved(membershipEvent);
        String masterId = manager.getNodeID(); // master node
        List<String> nodeIdList = manager.getNodes();
        if (nodeIdList.get(0).equals(masterId)) {
            manager.getAsyncMap(KeyDefine.CLUSTER_MAP_NAME, ar -> {
                if (ar.succeeded()) {
                    String downNodeId = membershipEvent.getMember().getUuid();
                    ar.result().get(downNodeId, obj -> {
                        JsonObject json = (JsonObject) obj.result();
                        if (json != null) {
                            System.out.printf("# yes I am manager, and the node %s offline!\n",
                                membershipEvent.getMember().toString());
                            System.out.println("## offline node info: " + obj.result().toString());
                            Boolean restartFlag = Boolean.parseBoolean(json.getString("restart", "false"));
                            if (restartFlag == null || restartFlag == false) return;
                            vertx.executeBlocking(h -> {
                                restart(ar.result(), nodeIdList, json);
                            }, r -> {
                                logger.info("restart node: {} ok", json.getString("nodeInfo"));
                            });
                        }
                    });
                }
            });
        }
    }
}

