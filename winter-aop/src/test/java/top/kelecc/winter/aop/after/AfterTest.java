package top.kelecc.winter.aop.after;


import org.junit.jupiter.api.Test;
import top.kelecc.winter.App;
import top.kelecc.winter.aop.after.pojo.AfterOriginBean;
import top.kelecc.winter.context.AnnotationConfigApplicationContext;
import top.kelecc.winter.io.PropertyResolver;
import top.kelecc.winter.util.YamlUtils;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 20:04
 */
public class AfterTest {
    @Test
    public void after() throws FileNotFoundException {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(map);
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        AfterOriginBean proxy = ctx.getBean("afterOriginBean");
        assertEquals("Hello, bavov14.后置通知", proxy.hello());
        assertEquals("Morning, bavov14.", proxy.morning());
        ctx.close();
    }
}
