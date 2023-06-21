package top.kelecc.component;

import org.junit.jupiter.api.Test;
import top.kelecc.App;
import top.kelecc.context.AnnotationConfigApplicationContext;
import top.kelecc.io.PropertyResolver;
import top.kelecc.util.YamlUtils;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/21 23:35
 */
public class BeanPostProcessorProxyTest {

    @Test
    public void InjectProxyTest() {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(map);
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        // 获取OriginBean的实例,此处获取的应该是SendProxyBeanProxy:
        OriginBean proxy = ctx.getBean(OriginBean.class);
        assertSame(SecondProxyBean.class, proxy.getClass());

        // proxy的name和version字段并没有被注入:
        assertNull(proxy.name);
        assertNull(proxy.version);

        // 但是调用proxy的getName()会最终调用原始Bean的getName(),从而返回正确的值:
        assertEquals("WinterFramework", proxy.getName());
        assertEquals("1.0", proxy.getVersion());

        // 获取InjectProxyOnConstructorBean实例:
        InjectProxyOnConstructorBean inject = ctx.getBean(InjectProxyOnConstructorBean.class);
        // 注入的OriginBean应该为Proxy，而且和前面返回的proxy是同一实例:
        assertSame(proxy, inject.injected);
        ctx.close();
    }

}
