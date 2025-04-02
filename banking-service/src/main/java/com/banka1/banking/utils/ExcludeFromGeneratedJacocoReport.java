package com.banka1.banking.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Retention(RetentionPolicy.CLASS)
@Target({TYPE, METHOD, CONSTRUCTOR})
public @interface ExcludeFromGeneratedJacocoReport {
    String value() default "";
}
