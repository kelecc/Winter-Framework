package top.kelecc.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kelecc.utils.WebUtils;
import top.kelecc.winter.context.AnnotationConfigApplicationContext;
import top.kelecc.winter.context.ApplicationContext;
import top.kelecc.winter.exception.NestedRuntimeException;
import top.kelecc.winter.io.PropertyResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/30 18:14
 */
public class ContextLoaderListener implements ServletContextListener {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("初始化 {}", getClass().getName());
        //获取Servlet容器
        ServletContext servletContext = sce.getServletContext();
        WebMvcConfiguration.setServletContext(servletContext);
        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        ApplicationContext ctx = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        WebUtils.registerFilters(servletContext);
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);
        servletContext.setAttribute("applicationContext", ctx);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("通过配置类: {} 初始化ApplicationContext", configClassName);
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("初始化ApplicationContext失败，因为缺少初始化参数：configuration!");
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("初始化ApplicationContext失败，因为无法正确加载初始化参数中的配置类: " + configClassName);
        }
        return new AnnotationConfigApplicationContext(clazz, propertyResolver);
    }
}
