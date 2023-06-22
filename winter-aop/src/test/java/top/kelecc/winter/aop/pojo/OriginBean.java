package top.kelecc.winter.aop.pojo;

import top.kelecc.winter.aop.annotation.Kele;

public class OriginBean {
    public String name;

    @Kele
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
