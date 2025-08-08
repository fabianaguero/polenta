package com.santec.polenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.santec.polenta.config.PrestoConfig;

@SpringBootApplication
@EnableConfigurationProperties(PrestoConfig.class)
public class PolentaMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolentaMcpServerApplication.class, args);
    }
}
