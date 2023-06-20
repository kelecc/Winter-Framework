package top.kelecc.config;

import top.kelecc.annotation.Bean;
import top.kelecc.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Configuration
public class DateTimeConfig {
    @Bean
    LocalDateTime local() { return LocalDateTime.now(); }

    @Bean(initMethod = "zonedInit",destroyMethod = "zonedDestroy")
    ZonedDateTime zoned() { return ZonedDateTime.now(); }

    public void zonedInit(){
        System.out.println("zonedInit.........................");
    }
    public void zonedDestroy(){
        System.out.println("zonedDestroy.........................");
    }
}
