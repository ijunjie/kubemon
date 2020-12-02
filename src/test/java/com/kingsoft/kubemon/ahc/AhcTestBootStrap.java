package com.kingsoft.kubemon.ahc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * AhcClientTest 启动类
 * 仅扫描 com.ksyun.kbdp.dts.service.ahc 包
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, QuartzAutoConfiguration.class})
@ComponentScan(basePackages = {"com.kingsoft.kubemon.ahc"})
public class AhcTestBootStrap {
    public static void main(String[] args) {
        SpringApplication.run(AhcTestBootStrap.class, args);
    }
}
