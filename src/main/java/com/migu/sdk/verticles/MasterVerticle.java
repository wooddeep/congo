package com.migu.sdk.verticles;

import com.migu.sdk.constant.Constant;
import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.protocol.*;
import com.migu.sdk.protocol.subcmd.SubCmd;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

import static com.migu.sdk.entry.Configure.sysConfig;


/**
 * Created by lihan on 2018/6/15.
 */


public class MasterVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        NetServer server = vertx.createNetServer(
            new NetServerOptions().setPort(Integer.parseInt(sysConfig.getString("proxyport", "47017"))));
        server.connectHandler(socket -> {
            socket.handler(buffer -> {
                MongoHead head = new MongoHead();
                byte[] in = buffer.getBytes();
                head.parse(in);

                switch (head.opCode) {
                    case Constant.MONGO_OPCODE_DELETE:
                        DeleteBody delete = new DeleteBody();
                        delete.parse(in);
                        SubCmdItf cmd = SubCmd.getSubCmd("delete");
                        cmd.response(vertx, delete, head, socket);
                        break;

                    case Constant.MONGO_OPCODE_UPDATE:
                        UpdateBody update = new UpdateBody();
                        update.parse(in);
                        cmd = SubCmd.getSubCmd("update");
                        cmd.response(vertx, update, head, socket);
                        break;

                    case Constant.MONGO_OPCODE_INSERT: // 插入 TODO
                        Insert insert = new Insert();
                        insert.parse(in);

                        SubCmdItf subCmdObj = SubCmd.getSubCmd("insert");
                        subCmdObj.response(vertx, insert, head, socket);

                        break;

                    case Constant.MONGO_OPCODE_QUERY: // 查询 及 协议交互
                        Query query = new Query();
                        query.parse(in);

                        if (query.fullCollectionName.contains("$cmd")) {
                            Reply reply = new Reply();
                            reply.setDocumentAndSend(vertx, query, head, socket);
                            break;
                        }

                        // find命令
                        subCmdObj = SubCmd.getSubCmd("find"); // TODO 根据metadata里面的配置, 到目的数据库中查询
                        subCmdObj.response(vertx, query, head, socket);

                        break;

                    default:
                        System.out.printf("## opcode: %d is unknown!\n", head.opCode);
                }
            });
        });

        server.listen();
    }
}
