package com.migu.sdk.entry;

import com.coveo.nashorn_modules.FilesystemFolder;
import com.coveo.nashorn_modules.Require;
import io.vertx.core.json.JsonObject;
import jdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;

import static com.migu.sdk.entry.CmdLine.cmlConfig;
import static com.migu.sdk.toolkit.LogUtil.loadLogConf;

/**
 * Created by lihan on 2018/2/5.
 */
public class Configure {

    public static JsonObject sysConfig = new JsonObject();

    public static JsonObject getConfig() {
        return sysConfig;
    }

    public static void execJavascript() throws Exception {
        String dir = cmlConfig.getString("conf");

        NashornScriptEngine nashorn = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
        FilesystemFolder rootFolder = FilesystemFolder.create(new File(dir), "UTF-8");
        Require.enable(nashorn, rootFolder);
        nashorn.eval(new FileReader(dir + "config.js"));
        Invocable invocable = (Invocable) nashorn;
        Object result = invocable.invokeFunction("getConfig");

        sysConfig = new JsonObject(result.toString());

        return;
    }

    private static void overrideCfg() throws Exception {
        for (int i = 0; i < CmdLine.opts.length; i++) {
            String name = CmdLine.opts[i].longOpt;
            String value = cmlConfig.getString(name);
            if (value != null) {
                if (CmdLine.opts[i].isString) {
                    sysConfig.put(name, value);
                } else {
                    //Integer ival = Integer.parseInt(value);
                    sysConfig.put(name, Integer.parseInt(value));
                }
            }
        }
    }

    public static void initSysConfig(String[] args) throws Exception {
        CmdLine.parseCmdline(args);
        execJavascript();
        overrideCfg();
        loadLogConf();
    }

}
