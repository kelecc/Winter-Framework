package top.kelecc.context;

import org.junit.jupiter.api.Test;
import top.kelecc.App;
import top.kelecc.io.PropertyResolver;

import java.util.Properties;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 20:07
 */
public class ApplicationContextTest {


    @Test
    public void scanTest(){
        Properties properties = new Properties();
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        System.out.println();
    }

}
