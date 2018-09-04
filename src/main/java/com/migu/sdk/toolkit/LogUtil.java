package com.migu.sdk.toolkit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.migu.sdk.entry.Configure.sysConfig;

/**
 * Created by lihan on 2018/02/08.
 * <p>
 * 参考资料
 * ------------------------------------------------------------------------
 * http://blog.csdn.net/nba20071786/article/details/52233434
 */
public class LogUtil {

    public static Logger fileAppenderDemo() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        Layout<? extends Serializable> layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, null, config, null,
            null, true, true, null, null);

        Appender appender = FileAppender.createAppender("log4jtest.txt", "true", "false", "File", "true",
            "false", "false", "4000", layout, null, "false", null, config);
        appender.start();

        config.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);

        AppenderRef[] refs = new AppenderRef[]{ref};

        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.INFO, "org.apache.logging.log4j",
            "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);


        config.addLogger("simpleTestLogger", loggerConfig);
        ctx.updateLoggers();


        Logger l = ctx.getLogger("simpleTestLogger");
        //l.info("message of info level shoud be output properly");
        //l.error("error message");

        return l;
    }

    public static void consoleAppenderDemo() {
        LoggerContext context = (LoggerContext) LogManager.getContext();
        Configuration config = context.getConfiguration();

        PatternLayout consoleLayout = PatternLayout.createLayout("%m%n", null, null, null, Charset.defaultCharset(), false, false, null, null);
        Appender consoleAppender = ConsoleAppender.createAppender(consoleLayout, null, null, "CONSOLE_APPENDER", null, null);
        consoleAppender.start();
        AppenderRef consoleRef = AppenderRef.createAppenderRef("CONSOLE_APPENDER", null, null);
        AppenderRef[] consoleRefs = new AppenderRef[]{consoleRef};
        LoggerConfig consoleLoggerConfig = LoggerConfig.createLogger("false", Level.INFO, "CONSOLE_LOGGER", "com", consoleRefs, null, null, null);
        consoleLoggerConfig.addAppender(consoleAppender, null, null);

        config.addAppender(consoleAppender);
        config.addLogger("com", consoleLoggerConfig);
        context.updateLoggers(config);

        Logger logger = LogManager.getContext().getLogger("com");
        logger.info("HELLO_WORLD");
    }

    // TODO
    public static Logger getLogger(String logTag, String logFilePath) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        Layout<? extends Serializable> layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, null, config, null,
            null, true, true, null, null);

        if (logTag == null || logTag.matches("\\s*")) logTag = "NA";
        if (logFilePath == null || logFilePath.matches("\\s*")) logFilePath = "untitle.log";

        String dftLogDirName = System.getProperty("user.dir") + "/logs/";
        File dftLogDir = new File(System.getProperty("user.dir") + "/logs/");
        if (!dftLogDir.exists()) dftLogDir.mkdir();
        String logDir = sysConfig.getString("logdir", dftLogDirName);

        if (logDir.charAt(0) == '.') {
            String currDir = System.getProperty("user.dir");
            logDir = currDir + File.separator + logDir;
        }

        char dirLastChar = logDir.charAt(logDir.length() - 1);
        char file1stChar = logFilePath.charAt(0);
        if (dirLastChar != '/' && dirLastChar != '\\' && file1stChar != '/' && file1stChar != '\\') {
            logDir = logDir + File.separator;
        }

        Appender appender = FileAppender.createAppender(logDir + logFilePath, "true", "false", "File", "true",
            "false", "true", "4000", layout, null, "false", null, config);

        PatternLayout consoleLayout = PatternLayout.createLayout("%m%n", null, null, null, Charset.defaultCharset(), false, false, null, null);
        Appender consoleAppender = ConsoleAppender.createAppender(consoleLayout, null, null, "CONSOLE_APPENDER", null, null);
        consoleAppender.start();

        config.addAppender(appender);
        config.addAppender(consoleAppender);

        AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
        AppenderRef consoleRef = AppenderRef.createAppenderRef("CONSOLE_APPENDER", null, null);

        AppenderRef[] refs = new AppenderRef[]{ref, consoleRef};

        LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.INFO, "org.apache.logging.log4j",
            "true", refs, null, config, null);
        //loggerConfig.addAppender(rollingAppender, null, null);

        loggerConfig.addAppender(appender, null, null);
        loggerConfig.addAppender(consoleAppender, null, null);

        config.addLogger(logTag, loggerConfig);
        ctx.updateLoggers();

        Logger logger = ctx.getLogger(logTag);

        return logger;
    }

    public static String getLogDir() {
        String dirStr = sysConfig.getString("logdir");

        if (dirStr == null) {
            dirStr = System.getProperty("user.dir") + File.separator + "logs" + File.separator;
        }

        if (dirStr.charAt(0) == '.') {
            dirStr = System.getProperty("user.dir") + File.separator + dirStr;
        }

        char lastChar = dirStr.charAt(dirStr.length() - 1);
        if (lastChar != '/' && lastChar != '\\') {
            dirStr = dirStr + File.separator;
        }

        File dirObj = new File(dirStr);
        if (!dirObj.exists()) dirObj.mkdir();

        return dirStr;
    }

    public static String getCfgDir() {
        String dirStr = sysConfig.getString("conf");

        if (dirStr == null) {
            dirStr = System.getProperty("user.dir") + File.separator + "conf" + File.separator;
        }

        if (dirStr.charAt(0) == '.') {
            dirStr = System.getProperty("user.dir") + File.separator + dirStr;
        }

        char lastChar = dirStr.charAt(dirStr.length() - 1);
        if (lastChar != '/' && lastChar != '\\') {
            dirStr = dirStr + File.separator;
        }

        File dirObj = new File(dirStr);
        if (!dirObj.exists()) dirObj.mkdir();

        return dirStr;
    }

    public static void loadLogConf() throws IOException {
        String logdir = getLogDir();
        //System.out.println(logdir);

        File file = new File(getCfgDir() + "log4j2.xml");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        int buffSize = Math.max(4096, in.available());
        ByteArrayOutputStream contents = new ByteArrayOutputStream(buffSize);
        byte[] buff = new byte[buffSize];
        for (int length = in.read(buff); length > 0; length = in.read(buff)) {
            contents.write(buff, 0, length);
        }
        String xmlStr = contents.toString();
        String logDirConf = String.format("%s%s%s", "<property name=\"LOG_HOME\">", logdir, "</property>")
            .replace("\\", "\\\\");
        xmlStr = xmlStr.replaceAll("<property name=\"LOG_HOME\">.*</property>", logDirConf);

        //System.out.println(xmlStr);

        ByteArrayInputStream bis = new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8));
        in = new BufferedInputStream(bis);
        ConfigurationSource source = new ConfigurationSource(in);
        Configurator.initialize(null, source);
    }

    public static void main(String[] args) throws IOException {

        //Logger l = getLogger("123", "");
        //l.info("message of info level shoud be output properly");
        //l.error("error message");
        //Logger logger = getLogger("1234", "xx.log");
        //logger.error("fatal error!");

        //loadLogConf();
    }

}
