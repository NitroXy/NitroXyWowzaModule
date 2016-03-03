package com.nitroxy.wmz.module;

import java.lang.annotation.*;

@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Exposed {
	String method() default "POST";
	String url() default "";
}
