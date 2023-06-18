package top.kelecc.io;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/18 16:21
 */
public class PropertyResolverTest {
    private PropertyResolver propertyResolver;
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void resolverTest() throws IOException {
        Properties props = new Properties();
        props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
        propertyResolver = new PropertyResolver(props);
        Map<Class<?>, Function<String, Object>> map = new HashMap<>();
        map.put(Resource.class, str -> new Resource(str,str));
        propertyResolver.registerConverter(map);
        LocalDate localDate = propertyResolver.getProperty("${localDate:2023-06-18}", LocalDate.class);
        if (localDate != null) {
            System.out.println(localDate);
        }
    }

}
