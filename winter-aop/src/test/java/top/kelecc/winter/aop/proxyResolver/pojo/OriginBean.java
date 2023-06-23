package top.kelecc.winter.aop.proxyResolver.pojo;

import top.kelecc.winter.annotation.Enhanced;
@Enhanced("allProxyHandler")
public class OriginBean {
    public String name;

    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
