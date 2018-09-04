package com.migu.sdk.protocol;

import com.migu.sdk.constant.ErrorCode;

/**
 * Created by lihan on 2018/2/13.
 */
@FunctionalInterface
public interface Callback {
    void callback(ErrorCode rc);
}