package top.kelecc.util;

import jakarta.annotation.Nullable;
import top.kelecc.exception.BeanDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 17:26
 */
public class ClassUtils {
    /**
     * 递归查找Annotation
     *
     * @param target          要查找的类
     * @param annotationClass 匹配的注解
     * @param <A>
     * @return 找到的注解
     */
    @Nullable
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annotationClass) {
        A annotation = target.getAnnotation(annotationClass);
        for (Annotation anno : target.getDeclaredAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!"java.lang.annotation".equals(annoType.getPackage().getName())) {
                A a = findAnnotation(annoType, annotationClass);
                if (a != null) {
                    if (annotation != null) {
                        throw new BeanDefinitionException(String.format("类：'%s' 上有重复的 '%s' 注解！", target.getName(), annotationClass.getName()));
                    }
                    annotation = a;
                }
            }
        }
        return annotation;
    }

    /**
     * 通过注解获取类中的方法
     * @param clazz
     * @param anno
     * @return
     */
    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> anno) {
        List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(anno))
                .map(method -> {
                    if (method.getParameterCount() != 0) {
                        throw new BeanDefinitionException(String.format("被 '%s' 标注的 '%s' 方法不允许有参数。 '%s'", anno.getName(), method.getName(), clazz.getName()));
                    }
                    return method;
                })
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() == 1) {
            return methods.get(0);
        }
        throw new BeanDefinitionException(String.format("在类 '%s' 中有多个方法被 '%s'注解标注！",clazz.getName(),anno.getName()));
    }
}
