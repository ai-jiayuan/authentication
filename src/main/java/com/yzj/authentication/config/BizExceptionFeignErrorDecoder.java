package com.yzj.authentication.config;

import com.alibaba.fastjson.JSONObject;
import com.uniccc.common.core.entity.vo.ResultDto;
import com.uniccc.common.core.exception.BaseException;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.uniccc.common.core.enums.ResultCode.SERVER_ERROR;

/**
 * @author wang
 */
@Slf4j
@Configuration
public class BizExceptionFeignErrorDecoder implements feign.codec.ErrorDecoder{

    @Override
    public Exception decode(String methodKey, Response response) {
        if(response.status() >= 400){
            String body = null;
            try (InputStream inputStream = response.body().asInputStream()){
                body = getString(inputStream);
            } catch (IOException e) {
                log.error("解析body出错");
            }
            if(!StringUtils.isBlank(body)) {
                JSONObject jsonObject = JSONObject.parseObject(body);
                if(Objects.nonNull(jsonObject.getBoolean("success"))) {
                    return new BaseException(SERVER_ERROR, jsonObject.getString("errorMessage"));
                }else if(Objects.nonNull(jsonObject.getInteger("code"))
                        &&Objects.nonNull(jsonObject.getString("message"))){
                    return new BaseException(jsonObject.toJavaObject(ResultDto.class));
                }
            }
        }
        return feign.FeignException.errorStatus(methodKey, response);
    }


    private String getString(InputStream inputStream){
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        String str = null;
        try {
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        str = result.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("读取body流出错");
        }
        return str;
    }

}
