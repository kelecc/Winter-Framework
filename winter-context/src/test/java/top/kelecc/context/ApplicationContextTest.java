package top.kelecc.context;

import org.junit.jupiter.api.Test;
import top.kelecc.App;
import top.kelecc.component.A;
import top.kelecc.io.PropertyResolver;
import top.kelecc.util.YamlUtils;

import java.util.Map;
import java.util.Properties;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 20:07
 */
public class ApplicationContextTest {


    @Test
    public void Test() {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(map);
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        A a = applicationContext.getBean("a");
        System.out.println(a);
        applicationContext.close();
    }

}
