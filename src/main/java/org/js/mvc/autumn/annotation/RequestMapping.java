package org.js.mvc.autumn.annotation;

import java.lang.annotation.*;

/**
 * Created by JiaShun on 2018/8/14.
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
