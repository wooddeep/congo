package com.migu.sdk.toolkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by lihan on 2018/2/27.
 */
public class ShellUtil {

    public static boolean listenPortExist(String[] portArr) throws Exception {
        String[] cmd = {"/bin/sh", "-c", "netstat -na | grep LISTEN"};
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            cmd[0] = "cmd";
            cmd[1] = "/c";
            cmd[2] = "netstat -na | findstr LISTEN";
        }

        Runtime runtime = Runtime.getRuntime();

        BufferedReader br = new BufferedReader(
            new InputStreamReader(runtime.exec(cmd).getInputStream())
        );

        String line = null;
        while ((line = br.readLine()) != null) {
            for (String port : portArr) {
                if (line.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+:" + port + ".*")) {
                    System.out.printf("## port: %s match!\n", port);
                    return true;
                }
            }
        }

        return false;
    }

    public static void listenPortExist(List<String> portLst) throws Exception {
        String[] cmd = {"/bin/sh", "-c", "netstat -na | grep LISTEN"};
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            cmd[0] = "cmd";
            cmd[1] = "/c";
            cmd[2] = "netstat -na | findstr LISTEN";
        }

        Runtime runtime = Runtime.getRuntime();
        BufferedReader br = new BufferedReader(
            new InputStreamReader(runtime.exec(cmd).getInputStream())
        );
        String line = null;
        //StringBuffer b = new StringBuffer();
        while ((line = br.readLine()) != null) {
            // b.append(line + "\n");
            // System.out.println(line);
            // TCP    0.0.0.0:135            0.0.0.0:0              LISTENING
            for (String port : portLst) {
                if (line.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+:" + port + ".*")) {
                    System.out.printf("## port: %s match!\n", port);
                }
            }
        }
        //System.out.println(b.toString());
    }

    public static void main(String[] args) {
        ShellUtil delp = new ShellUtil();
        try {
            delp.listenPortExist(new String[]{"22", "3306"});
        } catch (Exception e) {

        }
    }


}
