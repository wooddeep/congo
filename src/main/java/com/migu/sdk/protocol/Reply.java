package com.migu.sdk.protocol;


import com.migu.sdk.framework.SubCmdItf;
import com.migu.sdk.protocol.subcmd.SubCmd;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by lihan on 2018/6/15.
 */
public class Reply extends MongoBody {
    private static Logger logger = LogManager.getLogger("reply");

    public int responseFlags;//int32    // bit vector - see details below
    public long cursorID;//int64    // cursor id if client needs to do get more's
    public int startingFrom;//int32    // where in the cursor this reply is starting
    public int numberReturned;//int32    // number of documents in the reply
    public Object documents;//bson.D or byte [] // documents

    public Reply() {
        this.responseFlags = 8;
        this.cursorID = 0;
        this.startingFrom = 0;
        this.numberReturned = 1;
    }

    public void setDocumentAndSend(Vertx vertx, Query query, MongoHead head, NetSocket socket) {
        String subCmd = query.query.getFirstKey();
        SubCmdItf subCmdObj = SubCmd.getSubCmd(subCmd);
        subCmdObj.response(vertx, query, head, socket);
    }

}
