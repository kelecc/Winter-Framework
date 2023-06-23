package top.kelecc.winter.aop.before.handler;

import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.aop.BeforeInvocationHandlerAdapter;

import java.lang.reflect.Method;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 22:23
 */
@Component
public class BeforeSayHelloHandler extends BeforeInvocationHandlerAdapter {
    @Override
    public void before(Object proxy, Method method, Object[] args) {
        args[0] = "傻呗" + args[0];
    }
}
