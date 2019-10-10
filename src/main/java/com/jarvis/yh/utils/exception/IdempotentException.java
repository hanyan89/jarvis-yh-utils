package com.jarvis.yh.utils.exception;

import com.jarvis.lib.common.base.exception.BaseException;

public class IdempotentException extends BaseException {

    public IdempotentException(String message) {
        super("110", message, null);
    }

}
