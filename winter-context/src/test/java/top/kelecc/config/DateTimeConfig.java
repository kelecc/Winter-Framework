package top.kelecc.config;

import top.kelecc.winter.annotation.Bean;
import top.kelecc.winter.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Configuration
public class DateTimeConfig {
    @Bean
    LocalDateTime local() { return LocalDateTime.now(); }

    @Bean(initMethod = "zonedInit",destroyMethod = "zonedDestroy")
    ZonedDateTime zoned() { return ZonedDateTime.now(); }

    public void zonedInit(){
        System.out.println("ZonedDateTime初始化！");
    }
    public void zonedDestroy(){
        System.out.println("ZonedDateTime开始销毁...");
    }
}
