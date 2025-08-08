package com.santec.polenta;

import com.santec.polenta.service.PrestoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.santec.polenta.config.PrestoConfig;

@SpringBootApplication
@EnableConfigurationProperties(PrestoConfig.class)
public class PolentaMcpServerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(PolentaMcpServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PolentaMcpServerApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner initializeConnection(PrestoService prestoService) {
        return args -> {
            logger.info("Inicializando conexión a Presto...");
            try {
                boolean connected = prestoService.testConnection();
                if (connected) {
                    logger.info("Conexión a Presto establecida correctamente al inicio de la aplicación");
                } else {
                    logger.error("No se pudo establecer conexión a Presto al inicio de la aplicación");
                }
            } catch (Exception e) {
                logger.error("Error al inicializar la conexión a Presto: {}", e.getMessage(), e);
            }
        };
    }
}
