package top.kelecc.winter.aop.before;


import org.junit.jupiter.api.Test;
import top.kelecc.winter.App;
import top.kelecc.winter.aop.before.pojo.BeforeOriginBean;
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
public class BeforeTest {
    @Test
    public void before() {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(map);
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        BeforeOriginBean proxy = ctx.getBean("beforeOriginBean");
        assertEquals("Hello, 傻呗bavov14.", proxy.hello("bavov14"));
        assertEquals("Morning, bavov14.", proxy.morning("bavov14"));
        ctx.close();
    }
}
