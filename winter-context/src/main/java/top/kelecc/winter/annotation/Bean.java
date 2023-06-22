package top.kelecc.winter.annotation;

import java.lang.annotation.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 18:33
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Bean {
    /**
     * Bean名字，默认为方法名
     */
    String value() default "";

    String initMethod() default "";

    String destroyMethod() default "";

}
