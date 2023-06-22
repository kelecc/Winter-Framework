package top.kelecc.winter.annotation;

import java.lang.annotation.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 18:36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Component
public @interface Configuration {

    /**
     * bean名字，默认为类名的首字母小写的驼峰命名
     */
    String value() default "";

}
