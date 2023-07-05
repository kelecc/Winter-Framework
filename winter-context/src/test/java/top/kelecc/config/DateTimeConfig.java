package top.kelecc.config;

import top.kelecc.winter.annotation.Bean;
import top.kelecc.winter.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Configuration
public class DateTimeConfig {
    @Bean
    LocalDateTime local() { return LocalDateTime.now(); }

    @Bean
    ZonedDateTime zoned() { return ZonedDateTime.now(); }


}
