package top.kelecc.winter.aop.after.pojo;

import top.kelecc.winter.annotation.After;
import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.annotation.Enhanced;
import top.kelecc.winter.annotation.Value;

@Component
@Enhanced("afterSayHelloHandler")
public class AfterOriginBean {

    @Value("${gu.name}")
    public String name;

    @After
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
