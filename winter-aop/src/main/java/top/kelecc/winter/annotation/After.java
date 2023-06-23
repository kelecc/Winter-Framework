package top.kelecc.winter.annotation;

import java.lang.annotation.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 23:11
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Inherited
public @interface After {
}

