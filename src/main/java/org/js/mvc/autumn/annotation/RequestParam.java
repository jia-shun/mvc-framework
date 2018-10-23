package org.js.mvc.autumn.annotation;

import java.lang.annotation.*;

/**
 * @author JiaShun
 * @date 2018/8/14
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value() default "";
}
