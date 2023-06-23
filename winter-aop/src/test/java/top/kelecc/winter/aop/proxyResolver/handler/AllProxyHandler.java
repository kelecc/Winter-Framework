package top.kelecc.winter.aop.proxyResolver.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class AllProxyHandler implements InvocationHandler {
    @Override
    public Object invoke(Object bean, Method method, Object[] args) throws Throwable {
            String ret = (String) method.invoke(bean, args);
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!(被可乐修改！)";
            }
            return ret;
    }
}
