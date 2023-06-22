package top.kelecc.winter.aop;

import org.junit.jupiter.api.Test;
import top.kelecc.winter.aop.handler.PoliteInvocationHandler;
import top.kelecc.winter.aop.pojo.OriginBean;

import static org.junit.jupiter.api.Assertions.*;

class ProxyResolverTest {

    @Test
    void getInstance() {
        assertSame(ProxyResolver.getInstance(), ProxyResolver.getInstance());
    }

    @Test
    void creatProxy() {
        //原始Bean
        OriginBean originBean = new OriginBean();
        originBean.name = "bavov14";
        //调用原始Bean的hello();
        assertEquals("Hello, bavov14.", originBean.hello());

        //创建proxy
        ProxyResolver proxyResolver = ProxyResolver.getInstance();
        OriginBean proxyBean = proxyResolver.creatProxy(originBean, new PoliteInvocationHandler());

        System.out.println(proxyBean.getClass().getName());

        //proxy类和OriginBean不同
        assertNotSame(OriginBean.class, proxyBean.getClass());

        //proxyBean的name字段为null
        assertNull(proxyBean.name);

        //调用带@Kele的方法
        assertEquals("Hello, bavov14!(被可乐修改！)", proxyBean.hello());
        //不带@Kele的方法
        assertEquals("Morning, bavov14.", proxyBean.morning());

    }
}
