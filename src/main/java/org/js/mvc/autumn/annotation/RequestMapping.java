package org.js.mvc.autumn.annotation;

import java.lang.annotation.*;

/**
 * @author JiaShun
 * @date 2018/8/14
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
