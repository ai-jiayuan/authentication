package com.yzj.authentication.provider;


import com.yzj.authentication.param.CompanyDto;
import com.yzj.authentication.param.IdentityDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "xl-organization", fallback = OrganizationProviderFallback.class)
public interface OrganizationProvider {

    @GetMapping(value = "/visit")
    ResponseEntity<Boolean> isVisit(@RequestParam("username") String username, @RequestParam("url") String url, @RequestParam("method") String method, @RequestParam("clientId") String clientId, @RequestParam("ip") String ip);

    @GetMapping(value = "/default/identity/{username}/{clientId}")
    ResponseEntity<IdentityDto> getIdentityDto(@PathVariable("username") String username, @PathVariable("clientId") String clientId);

    @GetMapping(value = "/identity/all/{username}")
    ResponseEntity<List<IdentityDto>> getIdentityDtoList(@PathVariable("username") String username);

    @PutMapping(value = "/identity/{username}/{clientId}/{identityId}")
    ResponseEntity<IdentityDto> setIdentityDefault(@PathVariable("username") String username, @PathVariable("clientId") String clientId, @PathVariable("identityId") Long identityId);

    @GetMapping(value = "/company/{companyId}")
    ResponseEntity<CompanyDto> getCompanyDto(@PathVariable("companyId") Long companyId);
}
