package com.banka1.notification.aspect;

import com.banka1.common.model.Permission;
import com.banka1.common.model.Position;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorization {
    Permission[] permissions() default { };
    Position[] positions() default { };
    boolean allowIdFallback() default false;
}