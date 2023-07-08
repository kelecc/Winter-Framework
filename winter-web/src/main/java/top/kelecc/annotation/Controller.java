package top.kelecc.annotation;

import top.kelecc.winter.annotation.Component;

import java.lang.annotation.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/1 15:09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
}
