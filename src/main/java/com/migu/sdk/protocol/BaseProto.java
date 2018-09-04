package com.migu.sdk.protocol;

import com.migu.sdk.constant.ErrorCode;
import io.vertx.core.json.JsonObject;


import java.util.HashMap;
import java.util.Set;

/**
 * Created by lihan on 2018/2/27.
 */
public abstract class BaseProto {
    public abstract HashMap<String, Parameter> getParaMap();

    public abstract  String getCacheKey();

    public abstract JsonObject initCacheCtn();

    public abstract boolean cacheHit(JsonObject inCache);

    public abstract void prevHandler(JsonObject body);

    public abstract void postHandler(JsonObject body);

    public ErrorCode bodyHandler(JsonObject body) {
        HashMap<String, Parameter> paras = this.getParaMap();
        Set<String> keyList = paras.keySet(); // 以注册全量参数信息为基准
        for (String key : keyList) {
            Parameter paraDesc = paras.get(key);
            String value = paraDesc.getValue(body, key);
            boolean ret = paraDesc.checkValue(body, key, value);
            if (ret == false) return ErrorCode.PARA_CHK_FAIL;
            paraDesc.setValue(value);
            paraDesc.colNameMod(body);
        }
        return ErrorCode.SUCCESS;
    }
}
