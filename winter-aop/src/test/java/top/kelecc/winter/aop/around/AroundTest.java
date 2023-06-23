package top.kelecc.winter.aop.around;


import org.junit.jupiter.api.Test;
import top.kelecc.winter.App;
import top.kelecc.winter.aop.around.pojo.OriginBean;
import top.kelecc.winter.context.AnnotationConfigApplicationContext;
import top.kelecc.winter.io.PropertyResolver;
import top.kelecc.winter.util.YamlUtils;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 20:04
 */
public class AroundTest {
    @Test
    public void around() {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(map);
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        OriginBean proxy = ctx.getBean("originBean");
        assertEquals("Hello, 傻杯bavov14.今天下大雨啦！", proxy.hello("bavov14"));
        assertEquals("Morning, bavov14.", proxy.morning("bavov14"));
        ctx.close();
    }
}
