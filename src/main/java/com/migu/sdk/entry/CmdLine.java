package com.migu.sdk.entry;

import io.vertx.core.json.JsonObject;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import java.io.File;

/**
 * Created by lihan on 2018/2/5.
 */
class OptionDesc {
    public String opt;
    public String longOpt;
    public boolean hasArg;
    public String description;
    public boolean isString;

    public OptionDesc(String o, String l, boolean h, String d) {
        this.opt = o;
        this.longOpt = l;
        this.hasArg = h;
        this.description = d;
        this.isString = true;
    }

    public OptionDesc(String o, String l, boolean h, String d, boolean is) {
        this.opt = o;
        this.longOpt = l;
        this.hasArg = h;
        this.description = d;
        this.isString = is;
    }
}

public class CmdLine {
    // TODO cmlConfig 通过方法返回
    public static JsonObject cmlConfig = new JsonObject();

    public static OptionDesc[] opts = new OptionDesc[]{
        new OptionDesc("f", "conf", true, "Set the configure file dir"),
        new OptionDesc("h", "help", false, "Print this usage information"),
        new OptionDesc("t", "telport", true, "Set the telnet server's port", false),
        new OptionDesc("r", "restart", true, "Set the node's restart policy"),
        new OptionDesc("l", "logdir", true, "Set the log's directory"),
        new OptionDesc("m", "master", true, "Set the node as master!"),
        new OptionDesc("c", "cluster", true, "Set the node's cluster mode when run as master!"),
        new OptionDesc("d", "proxyport", true, "Set the master's proxy port! when node run as master mode!"),
        new OptionDesc("s", "mongod", true, "Set the mongodb's address and port when node run as worker mode!"),
        new OptionDesc("H", "clusterHost", true, "Set the cluster host"),
        new OptionDesc("L", "clusterList", true, "Set the cluster list"),
        new OptionDesc("z", "zklist", true, "Set the zookeeper list"),
    };

    public static void parseCmdline(String[] args) throws Exception {
        String currDir = System.getProperty("user.dir");
        String configDir = currDir + File.separator + "conf" + File.separator;
        CommandLineParser parser = new BasicParser();
        Options options = new Options();

        for (int i = 0; i < opts.length; i++) {
            OptionDesc option = opts[i];
            options.addOption(option.opt, option.longOpt, option.hasArg, option.description);
        }
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption('h')) {
            System.out.println("Usage: java -jar /path-to-jar/congo-(version)-SNAPSHOT.jar [cmd]");
            for (int i = 0; i < opts.length; i++) {
                OptionDesc option = opts[i];
                options.addOption(option.opt, option.longOpt, option.hasArg, option.description);
                System.out.println(String.format("\t-%s\t--%s\t%s!", option.opt, option.longOpt, option.description));
            }
            System.exit(0);
        }

        if (commandLine.hasOption('f')) {
            configDir = commandLine.getOptionValue('f');
            if (configDir.charAt(0) == '.') {
                configDir = currDir + File.separator + configDir;
                char lastChar = configDir.charAt(configDir.length() - 1);
                if (lastChar != '/' && lastChar != '\\') {
                    configDir = configDir + File.separator;
                }
            }
        }

        File configFile = new File(configDir + "config.js");
        if (!configFile.exists()) {
            System.out.printf("## the configure file: %s! don't exist, exit!\n", configDir + "config.js");
            System.exit(-1);
        }

        cmlConfig.put("conf", configDir);
        for (int i = 1; i < opts.length; i++) {
            OptionDesc option = opts[i];
            options.addOption(option.opt, option.longOpt, option.hasArg, option.description);
            String value = commandLine.getOptionValue(option.opt);
            if (value != null) cmlConfig.put(option.longOpt, commandLine.getOptionValue(option.opt));
        }
    }
}
