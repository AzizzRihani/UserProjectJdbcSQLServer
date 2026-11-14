package com.example.userprojectjdbcsqlserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// ConfigurationPropertiesScan : enregistre AppProperties (parametres d'initialisation, valides au demarrage).
@SpringBootApplication
@ConfigurationPropertiesScan
public class    UserProjectJdbcSqlServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserProjectJdbcSqlServerApplication.class, args);
    }
}
