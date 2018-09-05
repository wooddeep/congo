package com.migu.sdk.entry;

import com.hazelcast.config.*;
import com.migu.sdk.cluster.ClusterListener;
import com.migu.sdk.verticles.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import com.migu.tsg.SyncVerticle;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.migu.sdk.entry.Configure.sysConfig;

/**
 * Created by lihan on 2018/2/5.
 */
public class VertxEntry {

    private static Logger logger = LogManager.getLogger("frame");

    private static VertxOptions getClusterConfig() {

        String clusterHost = Configure.getConfig().getString("clusterHost");
        HazelcastClusterManager mgr = new HazelcastClusterManager();

        Config config = new Config();
        NetworkConfig netConfig = config.getNetworkConfig();
        JoinConfig joinConfig = netConfig.getJoin();
        MulticastConfig multicastConfig = joinConfig.getMulticastConfig();
        multicastConfig.setEnabled(false);

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        String[] addrs = Configure.getConfig().getString("clusterList").split(",");
        List<String> members = Arrays.stream(addrs).collect(Collectors.toList()); // 集群列表
        tcpIpConfig.setMembers(members);
        tcpIpConfig.setEnabled(true);

        mgr.setConfig(config);
        VertxOptions options = new VertxOptions().setClusterManager(mgr);
        options.setClustered(true);
        options.setClusterPublicHost(clusterHost);

        options.setHAEnabled(true);

        options.setHAGroup("congo");
        options.setQuorumSize(1);

        options.setWorkerPoolSize(Runtime.getRuntime().availableProcessors());
        options.setEventLoopPoolSize(Runtime.getRuntime().availableProcessors());

        return options;
    }

    private static VertxOptions getStandaloneConfig() {
        VertxOptions options = new VertxOptions();
        options.setWorkerPoolSize(Runtime.getRuntime().availableProcessors());
        options.setEventLoopPoolSize(Runtime.getRuntime().availableProcessors());
        return options;
    }

    private static void deployVerticles(Vertx vertx) {
        if (sysConfig.getString("master", "false").equals("true")) { // master
            vertx.deployVerticle(new MasterVerticle());

            vertx.deployVerticle(new WorkerVerticle(),
                new DeploymentOptions().setConfig(new JsonObject().put("database", "test")));
            vertx.deployVerticle(new ZkVerticle(), h -> { });
        } else { // slave
            String key = sysConfig.getString("mongod", "null");
            if (!key.matches("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+")) {
                logger.error("## key format error!");
                return;
            }

            vertx.deployVerticle(new ZkVerticle(), h -> {
                vertx.setTimer(10000, ih -> {
                    System.out.println("## add mongod!");
                    vertx.eventBus().send("ADD_WORKER", "/" + key.trim());
                });

                vertx.deployVerticle(new SyncVerticle());
            });
        }
    }

    public static void start() throws Exception {
        /*
        Integer port = sysConfig.getInteger("port");
        if (port == null) {
            System.out.printf("## port: %s error!\n", port);
            return;
        }
        Integer telport = sysConfig.getInteger("telport");
        if (telport == null) {
            System.out.printf("## telport: %s error!\n", telport);
            return;
        }

        String[] ports = new String[]{
            String.valueOf(sysConfig.getInteger("port")),
            String.valueOf(sysConfig.getInteger("telport"))
        };
        */
        //if (ShellUtil.listenPortExist(ports)) return;

        if (sysConfig.getString("mode", "standalone").equals("cluster")) { // 集群模式
            VertxOptions options = getClusterConfig();
            Vertx.clusteredVertx(options, res -> {
                if (res.succeeded()) {
                    Vertx vertx = res.result();
                    ClusterManager clusterManager = ((VertxInternal) vertx).getClusterManager();
                    HazelcastClusterManager manager = (HazelcastClusterManager) clusterManager;

                    manager.getHazelcastInstance().getCluster()
                        .addMembershipListener(new ClusterListener(manager, vertx));
                    deployVerticles(vertx);
                }
            });
        } else {
            VertxOptions options = getStandaloneConfig();
            options.setWarningExceptionTime(10l * 1000 * 1000000);
            options.setBlockedThreadCheckInterval(10000);

            Vertx vertx = Vertx.vertx(options);
            deployVerticles(vertx);
        }
    }
}
