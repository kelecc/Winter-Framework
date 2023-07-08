package top.kelecc.web;

import top.kelecc.winter.annotation.Autowired;
import top.kelecc.winter.annotation.Bean;
import top.kelecc.winter.annotation.Configuration;
import top.kelecc.winter.annotation.Value;

import javax.servlet.ServletContext;
import java.util.Objects;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/5 23:31
 */
@Configuration
public class WebMvcConfiguration {
    private static ServletContext servletContext = null;

    public static void setServletContext(ServletContext servletContext) {
        WebMvcConfiguration.servletContext = servletContext;
    }

    @Bean(initMethod = "init")
    ViewResolver viewResolver(@Autowired ServletContext servletContext,
                              @Value("${summer.web.freemarker.template-path:/WEB-INF/templates}") String templatePath,
                              @Value("${summer.web.freemarker.template-encoding:UTF-8}") String templateEncoding) {
        return new FreeMarkerViewResolver(templatePath, templateEncoding, servletContext);
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }
}
