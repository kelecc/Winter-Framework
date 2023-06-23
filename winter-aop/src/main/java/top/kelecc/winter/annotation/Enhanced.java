package top.kelecc.winter.annotation;

import java.lang.annotation.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 19:32
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface Enhanced {
    /**
     * Invocation handler bean name.
     */
    String value();
}
