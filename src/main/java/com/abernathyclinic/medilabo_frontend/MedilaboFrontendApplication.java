package com.abernathyclinic.medilabo_frontend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@SpringBootApplication
public class MedilaboFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedilaboFrontendApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attrs != null) {
                HttpServletRequest servletRequest = attrs.getRequest();
                String cookie = servletRequest.getHeader("Cookie");

                if (cookie != null) {
                    request.getHeaders().add("Cookie", cookie);
                }
            }

            return execution.execute(request, body);
        }));

        return restTemplate;
    }


}