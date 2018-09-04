package com.migu.sdk;

import com.migu.sdk.entry.Configure;
import com.migu.sdk.entry.VertxEntry;
import com.migu.sdk.framework.annotate.ClassParser;
import com.migu.sdk.framework.annotate.FrameCore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BSONObject;
import org.bson.BasicBSONEncoder;

import java.util.Set;

/**
 * Created by lihan on 2018/6/22.
 */
public class Congo {
    private static Logger logger = LogManager.getLogger("frame");

    public static void main(String[] args) {
        try {
            Set<Class<?>> clss = ClassParser.getClasses("com.migu.sdk.protocol.subcmd");
            FrameCore.prevHandle(clss);
            Configure.initSysConfig(args); // load configuration
            VertxEntry.start();  // actor perform
        } catch (Exception e) {
            StackTraceElement[] stes = e.getStackTrace();
            StringBuffer sb = new StringBuffer();
            for (StackTraceElement ste : stes) {
                sb.append("\r\n");
                sb.append(ste.toString());
            }
            logger.warn(sb.toString());
        }
    }
}
