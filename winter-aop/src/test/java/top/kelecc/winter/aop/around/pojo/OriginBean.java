package top.kelecc.winter.aop.around.pojo;

import top.kelecc.winter.annotation.Around;
import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.annotation.Enhanced;
import top.kelecc.winter.annotation.Value;

@Component
@Enhanced("aroundInvocationHandler")
public class OriginBean {

    @Value("${gu.name}")
    public String name;

    @Around
    public String hello(String uname) {
        return "Hello, " + uname + ".";
    }

    public String morning(String uname) {
        return "Morning, " + uname + ".";
    }
}
