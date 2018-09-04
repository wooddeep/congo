import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Created by lihan on 2018/2/10.
 */
public class RunVbsTest {

    //@Test
    public void test() throws Exception {
        try {
            String tempFilePath = "G:\\";
            File tempDir = new File(tempFilePath);
            //File temp = File.createTempFile(filename, ".csv", tempDir);

            // 创建临时文件
            File temp = File.createTempFile("run", ".vbs", tempDir);
            System.out.println(temp.getAbsoluteFile());
            //在程序退出时删除临时文件
            temp.deleteOnExit();
            // 向临时文件中写入内容
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write("Set ws = CreateObject(\"Wscript.Shell\") \n" +
                "ws.run \"cmd /c jar -jar F:\\work\\java\\ideal-ws\\sdk-mipay\\target\\sdk-mipay-0.0.1-SNAPSHOT.jar\", vbhide ");
            out.close();

            String[] cpCmd = new String[]{"wscript", temp.getAbsoluteFile().toString()};
            Process process = Runtime.getRuntime()
                .exec("java -jar F:\\work\\java\\ideal-ws\\sdk-mipay\\target\\sdk-mipay-0.0.1-SNAPSHOT.jar " +
                    "-p 8000 -t 9000 -C 127.0.0.1 -L 127.0.0.1");

            //val是返回值
            int val = process.waitFor();

            out.close();
        } catch (IOException e) {

        }
    }

    public void testB() {
        JsonObject json = new JsonObject("{\"key1\":\"val1\", \"key2\":\"val2\", \"key3\":\"v3\"}");
        //Object out = json.stream().filter((obj) -> !obj.getKey().equals("key1"))
        //    .collect(Collectors.toList()).stream().reduce((x, y) -> x.getValue() + y.getValue());
        //System.out.println(out);
        String cml = json.stream().map(obj -> "--" + obj.getKey() + " " + obj.getValue()).reduce((x, y) -> x + " " + y).get();
        System.out.println(cml);
    }

    @Test
    public void testC() {
        System.out.println(Boolean.parseBoolean("def"));
    }

}
