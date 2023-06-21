package top.kelecc.component;

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
    @Value("${b.name}")
    public String bName;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"bName\":").append(bName == null ? "" : "\"")
                .append(bName).append(bName == null ? "" : "\"");
        sb.append('}');
        return sb.toString();
    }
}
