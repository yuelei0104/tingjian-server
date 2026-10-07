package com.tingjian.usage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
public class UsageApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsageApplication.class, args);
    }

    @Bean
    Clock usageClock() {
        return Clock.system(UsageQuotaService.BUSINESS_ZONE);
    }
}
