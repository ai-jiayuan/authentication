package com.uniccc.authentication.provider;


import com.uniccc.authentication.param.CompanyDto;
import com.uniccc.authentication.param.IdentityDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationProviderFallback implements OrganizationProvider {

    @Override
    public ResponseEntity<Boolean> isVisit(String username, String url, String method, String clientId, String ip) {
        return ResponseEntity.ok().body(null);
    }

    @Override
    public ResponseEntity<IdentityDto> getIdentityDto(String username, String clientId) {
        return ResponseEntity.ok().body(null);
    }

    @Override
    public ResponseEntity<List<IdentityDto>> getIdentityDtoList(String username) {
        return ResponseEntity.ok().body(null);
    }

    @Override
    public ResponseEntity<IdentityDto> setIdentityDefault(String username, String clientId, Long identityId) {
        return ResponseEntity.ok().body(null);
    }

    @Override
    public ResponseEntity<CompanyDto> getCompanyDto(Long companyId) {
        return ResponseEntity.ok().body(new CompanyDto());
    }

}
