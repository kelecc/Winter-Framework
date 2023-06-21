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
    //Autowired注入
    @Autowired
    public B b;
    //构造注入
    public String age;
    //@Value属性注入
    @Value("${a.sex}")
    public String sex;
    //set注入
    public String tel;

    @Value("${a.name}")
    public String aName;

    //构造注入
    public A(@Value("${a.age}") String age) {
        this.age = age;
    }

    //set注入
    @Value("${a.tel}")
    public void setTel(String tel) {
        this.tel = tel;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"b\":")
                .append(b);
        sb.append(",\"age\":").append(age == null ? "" : "\"")
                .append(age).append(age == null ? "" : "\"");
        sb.append(",\"sex\":").append(sex == null ? "" : "\"")
                .append(sex).append(sex == null ? "" : "\"");
        sb.append(",\"tel\":").append(tel == null ? "" : "\"")
                .append(tel).append(tel == null ? "" : "\"");
        sb.append(",\"aName\":").append(aName == null ? "" : "\"")
                .append(aName).append(aName == null ? "" : "\"");
        sb.append('}');
        return sb.toString();
    }
}
