package top.kelecc.component;

import top.kelecc.annotation.Autowired;
import top.kelecc.annotation.Component;
import top.kelecc.annotation.Value;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/20 22:12
 */
@Component
public class C {

    public String cName;
    private A a;

    public C(@Value("${name.c}") String cName, @Autowired A a) {
        this.cName = cName;
        this.a = a;
    }
}
