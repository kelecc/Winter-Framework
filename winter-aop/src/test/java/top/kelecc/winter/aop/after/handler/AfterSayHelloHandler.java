package top.kelecc.winter.aop.after.handler;

import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.aop.AfterInvocationHandlerAdapter;

import java.lang.reflect.Method;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 22:23
 */
@Component
public class AfterSayHelloHandler extends AfterInvocationHandlerAdapter {
    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        return returnValue + "后置通知";
    }
}
