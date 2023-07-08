package top.kelecc.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kelecc.web.DispatcherServlet;
import top.kelecc.web.FilterRegistrationBean;
import top.kelecc.winter.context.ApplicationContext;
import top.kelecc.winter.context.ApplicationContextUtils;
import top.kelecc.winter.io.PropertyResolver;
import top.kelecc.winter.util.ClassPathUtils;
import top.kelecc.winter.util.YamlUtils;

import javax.servlet.*;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/30 18:25
 */
public class WebUtils {
    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);
    static final String CONFIG_APP_YAML = "application.yaml";
    static final String CONFIG_APP_PROP = "application.properties";
    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";

    public static PropertyResolver createPropertyResolver() {
        Properties properties = new Properties();
        try {
            Map<String, Object> yamlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            logger.info("加载配置文件: {}", CONFIG_APP_YAML);
            properties.putAll(yamlMap);
        } catch (FileNotFoundException e) {
            ClassPathUtils.readInputStream(CONFIG_APP_PROP, input -> {
                logger.info("加载配置文件: {}", CONFIG_APP_PROP);
                properties.load(input);
                return true;
            });
        }
        return new PropertyResolver(properties);
    }


    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver) {
        Servlet dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), propertyResolver);
        logger.info("注册servlet {} 映射URL '/'.", dispatcherServlet.getClass().getName());
        ServletRegistration.Dynamic dispatcherServletReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherServletReg.addMapping("/");
        dispatcherServletReg.setLoadOnStartup(0);
    }

    public static void registerFilters(ServletContext servletContext) {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        for (FilterRegistrationBean filterRegistrationBean : ctx.getBeans(FilterRegistrationBean.class)) {
            List<String> urlPatterns = filterRegistrationBean.getUrlPatterns();
            if (urlPatterns == null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("No url patterns for " + filterRegistrationBean.getClass().getName());
            }
            Filter filter = Objects.requireNonNull(filterRegistrationBean.getFilter(), "FilterRegistrationBean.getFilter() must not return null.");
            logger.info("注册filter '{}' {}, 过滤 '{}'。", filterRegistrationBean.getName(), filter.getClass().getName(), String.join(",", urlPatterns));
            FilterRegistration.Dynamic dynamic = servletContext.addFilter(filterRegistrationBean.getName(), filter);
            dynamic.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns.toArray(new String[0]));
        }
    }
}
