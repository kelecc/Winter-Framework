package top.kelecc.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.kelecc.winter.io.PropertyResolver;

public class WebUtilsTest {

    @Test
    void createPropertyResolver() {
        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        Assertions.assertEquals("root", propertyResolver.getProperty("winter.datasource.username"));
    }
}
