package top.kelecc.component;

import top.kelecc.annotation.Autowired;
import top.kelecc.annotation.Component;
import top.kelecc.annotation.Value;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/20 22:11
 */
@Component
public class A {
    public B b;


    public A(@Autowired B b, @Value("${name.a}") String aName) {
        this.b = b;
        this.aName = aName;
    }

    public String aName;

}
