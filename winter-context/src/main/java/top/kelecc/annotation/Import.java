package top.kelecc.annotation;

import java.lang.annotation.*; /**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 20:53
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Import {
    Class<?>[] value();
}
