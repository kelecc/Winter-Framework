package top.kelecc.annotation;

import top.kelecc.utils.WebUtils;

import java.lang.annotation.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/1 23:42
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

    String value();

    String defaultValue() default WebUtils.DEFAULT_PARAM_VALUE;
}
