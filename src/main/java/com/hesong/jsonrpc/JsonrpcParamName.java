package com.hesong.jsonrpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonrpcParamName {
    
    /**
     * The value of the parameter name.
     * @return the value
     */
    String value();
}
