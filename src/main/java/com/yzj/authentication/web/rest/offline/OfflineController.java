package com.yzj.authentication.web.rest.offline;

import com.netflix.discovery.DiscoveryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api")
public class OfflineController {
    private final Logger log = LoggerFactory.getLogger(OfflineController.class);

    @GetMapping("/offline")
    public void offline(){
        log.info("xl-auth开始从Eureka中剔除！！！");
        DiscoveryManager.getInstance().shutdownComponent();
        log.info("xl-auth从Eureka中剔除结束！！！");
    }
}
