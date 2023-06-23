package top.kelecc.winter.aop.before.pojo;

import top.kelecc.winter.annotation.Before;
import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.annotation.Enhanced;
import top.kelecc.winter.annotation.Value;

@Component
@Enhanced("beforeSayHelloHandler")
public class BeforeOriginBean {

    @Value("${gu.name}")
    public String name;

    @Before
    public String hello(String uname) {
        return "Hello, " + uname + ".";
    }

    public String morning(String uname) {
        return "Morning, " + uname + ".";
    }
}
