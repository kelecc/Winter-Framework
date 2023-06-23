package top.kelecc.winter.aop;

import top.kelecc.winter.annotation.Around;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 23:29
 */
public abstract class AroundInvocationHandlerAdapter implements InvocationHandler {
    /**
     * 前置通知
     *
     * @param proxy
     * @param method
     * @param args
     */
    public abstract void before(Object proxy, Method method, Object[] args);

    /**
     * 后置通知
     *
     * @param proxy
     * @param returnValue
     * @param method
     * @param args
     * @return
     */
    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Around around = method.getAnnotation(Around.class);
        if (around == null) {
            return method.invoke(proxy,args);
        }
        before(proxy, method, args);
        Object ret = method.invoke(proxy, args);
        return after(proxy, ret, method, args);
    }
}
