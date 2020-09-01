package com.uniccc.authentication;


import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients
@SpringCloudApplication
@EnableMethodCache(basePackages = "com.uniccc.authentication")
@EnableCreateCacheAnnotation
@ComponentScan(basePackages = {"com.uniccc"})
public class XlAuthenticationApplication {

	public static void main(String[] args) {
		SpringApplication.run(XlAuthenticationApplication.class, args);
	}

}
