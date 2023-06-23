package top.kelecc.winter.aop.around.handler;

import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.aop.AroundInvocationHandlerAdapter;

import java.lang.reflect.Method;

@Component
public class AroundInvocationHandler extends AroundInvocationHandlerAdapter {
    @Override
    public void before(Object proxy, Method method, Object[] args) {
        args[0] = "傻杯" + args[0];
    }

    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        return returnValue + "今天下大雨啦！";
    }
}
