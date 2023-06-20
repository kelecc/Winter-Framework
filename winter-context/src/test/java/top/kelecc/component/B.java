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
public class B {
    private C c;

    public String bName;

    public B(@Autowired C c, @Value("${name.b}") String bName) {
        this.c = c;
        this.bName = bName;
    }
}
