package top.kelecc.winter.aop;

import top.kelecc.winter.annotation.After;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 22:20
 */
public abstract class AfterInvocationHandlerAdapter implements InvocationHandler {
    /**
     * 后置通知
     * @param proxy
     * @param returnValue
     * @param method
     * @param args
     * @return
     */
    public abstract Object after(Object proxy, Object returnValue, Method method, Object[] args);

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        After after = method.getAnnotation(After.class);
        if (after != null) {
            Object ret = method.invoke(proxy, args);
            return after(proxy, ret, method, args);
        }
        return method.invoke(proxy, args);
    }
}
