package top.kelecc;

import org.junit.jupiter.api.Test;
import top.kelecc.io.ResourceResolver;

import java.util.List;

/**
 * Unit test for simple App.
 */
public class ResourceResolverTest {

    /**
     * 扫描类名测试
     */
    @Test
    public void scanTest(){
        ResourceResolver resourceResolver = new ResourceResolver("org.slf4j");
        List<String> scan = resourceResolver.scan(resource -> {
            return null;
        });
    }

}
