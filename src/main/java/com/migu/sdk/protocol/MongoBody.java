package com.migu.sdk.protocol;

import com.migu.sdk.toolkit.NumberUtil;
import org.bson.RawBsonDocument;
import org.joor.Reflect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lihan on 2018/6/15.
 */
public class MongoBody {

    private int strTail(byte[] arr, int start) {
        for (int i = start; ; i++) {
            if (arr[i] == 0) {
                return i;
            }
        }
    }

    public void parse(byte[] arr) {
        Map<String, Reflect> fieldMap = Reflect.on(this).fields();
        int[] index = new int[]{16};

        for (Map.Entry<String, Reflect> entry : fieldMap.entrySet()) {
            String key = entry.getKey();
            Reflect reflect = entry.getValue();

            if (index[0] >= arr.length) return;

            if (reflect.type() == int.class) {
                int intVal = NumberUtil.byte4ToIntLittle(arr, index[0]);
                Reflect.on(this).set(key, intVal);
                index[0] = index[0] + 4;

            }

            if (reflect.type() == java.lang.String.class) {
                int tailIndex = strTail(arr, index[0]);
                int length = tailIndex - index[0] + 1;
                byte[] strArr = new byte[length];
                System.arraycopy(arr, index[0], strArr, 0, length);
                Reflect.on(this).set(key, new String(strArr));
                index[0] = index[0] + length;
            }

            if (reflect.type() == RawBsonDocument.class) {
                int docLen = NumberUtil.byte4ToIntLittle(arr, index[0]);
                RawBsonDocument doc = new RawBsonDocument(arr, index[0], docLen);
                Reflect.on(this).set(key, doc);
                index[0] = index[0] + docLen;

            }
        }
    }

    public byte[] pack(int reqId, int resTo, int opCode) {
        byte[] head = new byte[16];
        Map<String, Reflect> fieldMap = Reflect.on(this).fields();
        byte[] arr0 = NumberUtil.intToByte4Little(reqId);
        System.arraycopy(arr0, 0, head, 4, 4);
        byte[] arr1 = NumberUtil.intToByte4Little(resTo);
        System.arraycopy(arr1, 0, head, 8, 4);
        byte[] arr2 = NumberUtil.intToByte4Little(opCode);
        System.arraycopy(arr2, 0, head, 12, 4);


        List<byte[]> arrList = new ArrayList<>();
        int length = 0;
        for (Map.Entry<String, Reflect> entry : fieldMap.entrySet()) {
            String key = entry.getKey();
            Reflect reflect = entry.getValue();

            if (reflect.type() == int.class) {
                byte[] arr = NumberUtil.intToByte4Little(reflect.on(this).get(key));
                length = length + arr.length;
                arrList.add(arr);
            }

            if (reflect.type() == long.class) {
                byte[] arr = NumberUtil.longToByte8Little(reflect.on(this).get(key));
                length = length + arr.length;
                arrList.add(arr);
            }

            if (reflect.type() == java.lang.String.class) {
                String str = reflect.on(this).get(key);
                byte[] arr = str.getBytes();
                length = length + arr.length;
                arrList.add(arr);
            }

            if (reflect.on(this).get(key) instanceof RawBsonDocument) {
                RawBsonDocument doc = reflect.on(this).get(key);
                byte[] arr = doc.getByteBuffer().array();
                length = length + arr.length;
                arrList.add(arr);
            }

            if (reflect.on(this).get(key) instanceof byte []) {
                byte[] arr = reflect.on(this).get(key);
                length = length + arr.length;
                arrList.add(arr);
            }
        }

        byte[] arr3 = NumberUtil.intToByte4Little(length + 16);
        System.arraycopy(arr3, 0, head, 0, 4);
        arrList.add(0, head); // 添加头

        byte[] out = new byte[length + 16];
        int index = 0;
        for (int i = 0; i < arrList.size(); i++) {
            System.arraycopy(arrList.get(i), 0, out, index, arrList.get(i).length);
            index = index + arrList.get(i).length;
        }

        return out;
    }
}
