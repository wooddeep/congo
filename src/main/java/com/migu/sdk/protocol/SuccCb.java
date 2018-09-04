package com.migu.sdk.protocol;

import com.migu.sdk.constant.ErrorCode;

/**
 * Created by lihan on 2018/3/9.
 */
@FunctionalInterface
public interface SuccCb {
    void callback(ErrorCode rc, Object out);
}
