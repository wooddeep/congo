package com.migu.sdk.protocol;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by lihan on 2018/2/9.
 */
public class Parameter {

    public Logger logger = LogManager.getLogger("protocol");
    public boolean isPrimKey = false;
    public boolean mustInMsg = true;
    public String name;
    public String colName = null;
    public ParaType type = ParaType.STRING;
    public ParaType tInDb = ParaType.STRING;
    public String pattern = null;
    public CheckFunc handler = null;
    public String value = null;

    public Parameter(String colName, ParaType type, boolean mustInMsg) {
        this.colName = colName;
        this.type = type;
        this.pattern = null;
        this.mustInMsg = mustInMsg;
    }

    public Parameter(String colName, ParaType type, String patt, boolean isPrimKey) {
        this.colName = colName;
        this.isPrimKey = isPrimKey;
        this.type = type;
        this.pattern = patt;
        this.handler = null;
    }

    public Parameter(String colName, ParaType type, ParaType tInDb, String patt, boolean isPrimKey) {
        this.colName = colName;
        this.isPrimKey = isPrimKey;
        this.type = type;
        this.tInDb = tInDb;
        this.pattern = patt;
        this.handler = null;
    }

    public Parameter(String colName, ParaType type, String patt) {
        this.colName = colName;
        this.type = type;
        this.pattern = patt;
        this.handler = null;
    }

    public Parameter(ParaType type, String patt) {

        //this.colName = colName;
        this.type = type;
        this.pattern = patt;
        this.handler = null;
    }

    public Parameter(String name, String colName, ParaType type, String patt, CheckFunc func) {
        this.name = name;
        this.colName = colName;
        this.type = type;
        this.pattern = patt;
        this.handler = func;
    }

    public boolean specCheck(JsonObject o) {
        return true;
    } // 参数特殊检查

    public void colNameMod(JsonObject o) {}

    public String getValue(JsonObject obj, String name) {
        String ret = null;
        switch (this.type) {
            case STRING:
                ret = obj.getString(name);
                break;
            case INT:
                Integer val = obj.getInteger(name, null);
                ret = val == null ? null : String.valueOf(val);
                break;
        }
        return ret;
    }

    public boolean checkValue(JsonObject json, String name, String valStr) {
        if (valStr == null && this.mustInMsg) {
            logger.info("## parameter: {} require! but not found", name);
            return false;
        }

        if (pattern != null) {
            boolean patChkRet = valStr.matches(this.pattern);
            if (!patChkRet) {
                logger.info("## parameter: {} error! require: {} but found: {}",
                    name, this.pattern, valStr);
                return false;
            }
        }

        boolean ret = this.specCheck(json);
        if (ret == false) {
            logger.info("## parameter: {} error! real-value: {}", name, valStr);
            return false;
        }

        this.value = valStr;
        return true;
    }

    public boolean setValue(String valStr) {
        return true;
    }

}
