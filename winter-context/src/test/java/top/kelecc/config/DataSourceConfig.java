package top.kelecc.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import top.kelecc.annotation.Component;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/20 16:09
 */
@Component
public class DataSourceConfig {
    @PreDestroy
    public void preDestroy(){
        System.out.println("preDestroy........................");
    }
    @PostConstruct
    public void postConstruct(){
        System.out.println("postConstruct........................");
    }
}
