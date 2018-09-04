package com.migu.sdk.protocol.subcmd;

import com.migu.sdk.framework.SubCmdItf;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by lihan on 2018/6/27.
 */
public class SubCmd {

    private static ConcurrentHashMap<String, SubCmdItf> subCmdMap = new ConcurrentHashMap<>();

    public static void regSubCmd(String cmdname, SubCmdItf subcmd) {
        subCmdMap.put(cmdname, subcmd);
    }

    public static SubCmdItf getSubCmd(String cmdname) {
        return subCmdMap.get(cmdname);
    }
}
