package com.bolsadeideas.springboot.weblfux.eureka.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class SpringBootWefluxEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWefluxEurekaServerApplication.class, args);
    }

}
