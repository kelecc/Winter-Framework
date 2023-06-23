package top.kelecc.winter.aop;

import top.kelecc.winter.annotation.Before;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 22:20
 */
public abstract class BeforeInvocationHandlerAdapter implements InvocationHandler {
    /**
     * 前置通知
     *
     * @param proxy
     * @param method
     * @param args
     */
    public abstract void before(Object proxy, Method method, Object[] args);

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Before before = method.getAnnotation(Before.class);
        if (before != null) {
            before(proxy, method, args);
        }
        return method.invoke(proxy, args);
    }
}
