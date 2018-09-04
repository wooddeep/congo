package com.migu.sdk.constant;

/**
 * Created by lihan on 2018/2/9.
 */


public enum ErrorCode {

    SUCCESS("0000", "成功"),
    SET_SUCCESS("0000", "设置成功"),
    CHK_SUCCESS("0000", "验证成功"),

    /**
     * protocol层错误码
     **/
    USER_SET_FAIL("0001", "设置失败"),
    PASS_CHK_FAIL("0002", "验证失败，密码/账号错误"),
    USER_NOT_REG("0003", "验证失败，密码未设置"),
    BODY_PATT_ERR("0004", "系统繁忙"), // 报文格式错误
    PARA_CHK_FAIL("0005", "参数校验错误"),

    /**
     * dao层错误码
     **/
    CONN_GET_FAIL("1001", "获取数据库连接失败"),
    DB_UP_FAIL("1002", "数据库update操作失败"),
    DB_QRY_FAIL("1003", "数据库query操作失败"),
    DB_NOT_FOUND("1004", "数据未找到"),

    /**
     * cache层错误码
     **/
    CACHE_GET_FAIL("2001", "缓存获取失败"),
    CACHE_DEL_FAIL("2001", "缓存删除失败"),

    END("", "");

    private String code;
    private String desc;

    private ErrorCode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
