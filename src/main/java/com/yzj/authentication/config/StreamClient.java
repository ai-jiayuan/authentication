package com.yzj.authentication.config;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;


public interface StreamClient {

    String JWT = "jwtClear";

    String BAN = "banUser";


    @Input(StreamClient.JWT)
    SubscribableChannel clear();

    @Input(StreamClient.BAN)
    SubscribableChannel banUser();

}
