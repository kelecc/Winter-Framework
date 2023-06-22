package top.kelecc.winter.aop.handler;

import top.kelecc.winter.aop.annotation.Kele;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class PoliteInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object bean, Method method, Object[] args) throws Throwable {
        // 修改标记了@Kele的方法返回值:
        if (method.getAnnotation(Kele.class) != null) {
            String ret = (String) method.invoke(bean, args);
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!(被可乐修改！)";
            }
            return ret;
        }
        return method.invoke(bean, args);
    }
}
