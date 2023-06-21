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
        System.out.println("数据库连接已全部释放！");
    }
    @PostConstruct
    public void postConstruct(){
        System.out.println("数据源初始化完成，我用的MySQL!");
    }
}
