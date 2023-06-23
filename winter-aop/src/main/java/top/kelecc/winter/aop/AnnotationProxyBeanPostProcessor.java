package top.kelecc.winter.aop;

import top.kelecc.winter.context.ApplicationContextUtils;
import top.kelecc.winter.context.BeanDefinition;
import top.kelecc.winter.context.BeanPostProcessor;
import top.kelecc.winter.context.ConfigurableApplicationContext;
import top.kelecc.winter.exception.AopConfigException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 23:58
 */
public abstract class AnnotationProxyBeanPostProcessor<A extends Annotation> implements BeanPostProcessor {
    Map<String, Object> originBeans = new HashMap<>();
    Class<A> annotationClass;
    public AnnotationProxyBeanPostProcessor() {
        this.annotationClass = getParameterizedType();
    }
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        A anno = beanClass.getAnnotation(annotationClass);
        if (anno != null) {
            String handlerName;
            try {
                handlerName = (String) anno.annotationType().getMethod("value").invoke(anno);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(String.format("@%s 必须有value()方法且返回值是String.", this.annotationClass.getSimpleName()), e);
            }
            Object proxy = this.createProxy(beanClass,bean,handlerName);
            originBeans.put(beanName,bean);
            return proxy;
        }
        return bean;
    }

    private Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        BeanDefinition beanDefinition = ctx.findBeanDefinition(handlerName);
        if (Objects.isNull(beanDefinition)) {
            throw new AopConfigException(String.format("处理bean: %s 时未找到handler: '%s'",beanClass.getName(),handlerName));
        }
        Object handler = beanDefinition.getInstance();
        if (Objects.isNull(handler)) {
            handler = ctx.createBeanAsEarlySingleton(beanDefinition);
        }
        if (handler instanceof InvocationHandler) {
            return ProxyResolver.getInstance().creatProxy(bean, (InvocationHandler) handler);
        } else {
            throw new AopConfigException(String.format("处理bean: %s 时handler实例化异常，@Around中标注的类不是InvocationHandler实现类！",beanClass.getName()));
        }
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object originBean = this.originBeans.get(beanName);
        return originBean == null ? bean : originBean;
    }

    @SuppressWarnings("unchecked")
    private Class<A> getParameterizedType() {
        Type type = getClass().getGenericSuperclass();
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        ParameterizedType pt = (ParameterizedType) type;
        Type[] types = pt.getActualTypeArguments();
        if (types.length != 1) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type r = types[0];
        if (!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }
}
