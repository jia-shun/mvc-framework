package org.js.mvc.autumn.annotation;

import java.lang.annotation.*;

/**
 * Created by JiaShun on 2018/8/14.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}
