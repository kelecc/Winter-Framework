package top.kelecc.winter.context;

import jakarta.annotation.Nullable;
import top.kelecc.winter.exception.BeanCreationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 14:05
 */
public class BeanDefinition implements Comparable<BeanDefinition> {
    /**
     * 全局唯一的Bean Name:
     */
    private final String name;

    /**
     * Bean的声明类型:
     */
    private final Class<?> beanClass;

    /**
     * Bean的实例:
     */
    private Object instance = null;

    /**
     * 构造方法/null:
     */
    private final Constructor<?> constructor;

    /**
     * 工厂方法名称/null:
     */
    private final String factoryName;

    /**
     * 工厂方法/null:
     */
    private final Method factoryMethod;

    /**
     * Bean的顺序:
     */
    private final int order;

    /**
     * 是否标识@Primary:
     */
    private final boolean primary;

    private boolean init = false;

    /**
     * init/destroy方法名称:
     */
    String initMethodName;
    String destroyMethodName;

    /**
     * init/destroy方法:
     */
    Method initMethod;
    Method destroyMethod;

    public BeanDefinition(String name, Class<?> beanClass, Constructor<?> constructor, int order, boolean primary, String initMethodName,
                          String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    public BeanDefinition(String name, Class<?> beanClass, String factoryName, Method factoryMethod, int order, boolean primary, String initMethodName,
                          String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }


    /**
     * 初始化initMethod和destroyMethod
     *
     * @param initMethodName
     * @param destroyMethodName
     * @param initMethod
     * @param destroyMethod
     */
    private void setInitAndDestroyMethod(String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    public String getName() {
        return name;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Nullable
    public Object getInstance() {
        return instance;
    }

    public Object getRequiredInstance() {
        if (this.instance == null) {
            throw new BeanCreationException(String.format("名字为： '%s' ，类型为： '%s' 的bean当前阶段未实例化！",
                    this.getName(), this.getBeanClass().getName()));
        }
        return this.instance;
    }

    public void setInstance(Object instance) {
        Objects.requireNonNull(instance, "实例为null!");
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
            throw new BeanCreationException(String.format("期待的类型为 '%s'，而不是 '%s' ",
                    this.beanClass.getName(), instance.getClass().getName()));
        }
        this.instance = instance;
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Nullable
    public String getFactoryName() {
        return factoryName;
    }

    @Nullable
    public Method getFactoryMethod() {
        return factoryMethod;
    }

    public int getOrder() {
        return order;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit() {
        this.init = true;
    }

    @Nullable
    public String getInitMethodName() {
        return initMethodName;
    }

    @Nullable
    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    @Nullable
    public Method getInitMethod() {
        return initMethod;
    }

    @Nullable
    public Method getDestroyMethod() {
        return destroyMethod;
    }

    @Override
    public String toString() {
        return "BeanDefinition [name=" + name + ", beanClass=" + beanClass.getName() + ", factory=" + getCreateDetail() + ", init-method="
                + (initMethod == null ? "null" : initMethod.getName()) + ", destroy-method=" + (destroyMethod == null ? "null" : destroyMethod.getName())
                + ", primary=" + primary + ", instance=" + instance + "]";
    }

    String getCreateDetail() {
        if (this.factoryMethod != null) {
            String params = String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new));
            return this.factoryMethod.getDeclaringClass().getSimpleName() + "." + this.factoryMethod.getName() + "(" + params + ")";
        }
        return null;
    }

    @Override
    public int compareTo(BeanDefinition def) {
        int cmp = Integer.compare(this.order, def.order);
        if (cmp != 0) {
            return cmp;
        }
        return this.name.compareTo(def.name);
    }
}
