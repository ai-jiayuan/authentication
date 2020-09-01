package com.uniccc.authentication.config;

import feign.Feign;
import okhttp3.ConnectionPool;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@AutoConfigureBefore(FeignAutoConfiguration.class)
@Configuration
@ConditionalOnClass(Feign.class)
/****
 *     需要修改成OKHTTP的客户端，需要在配置文件增加
 *     feign.httpclient.enabled=false
	   feign.okhttp.enabled=true
 */
public class FeignOkHttpConfig {

	private int feignOkHttpReadTimeout = 60;
	private int feignConnectTimeout = 60;
	private int feignWriteTimeout = 120;

	@Bean
	public okhttp3.OkHttpClient okHttpClient() {
		return new okhttp3.OkHttpClient.Builder()
				.readTimeout(feignOkHttpReadTimeout, TimeUnit.SECONDS)
				.connectTimeout(feignConnectTimeout, TimeUnit.SECONDS)
				.writeTimeout(feignWriteTimeout, TimeUnit.SECONDS)
				//自定义链接池
				.connectionPool(new ConnectionPool(1000, 10,  TimeUnit.MINUTES))
//				.addInterceptor(XXXXXXXInterceptor) 	//自定义拦截器
				.build();
	}
}
