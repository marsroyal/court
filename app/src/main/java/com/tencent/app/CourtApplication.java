package com.tencent.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(basePackages = {"com.tencent.core.mapper"})
@ComponentScan(basePackages = {"com.tencent.*"})
public class CourtApplication {

  public static void main(String[] args) {
    SpringApplication.run(CourtApplication.class, args);
  }
}
