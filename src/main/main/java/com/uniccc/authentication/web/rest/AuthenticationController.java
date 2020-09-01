package com.uniccc.authentication.web.rest;


import com.uniccc.authentication.param.IdentityDto;
import com.uniccc.authentication.param.TokenMsgDto;
import com.uniccc.authentication.provider.OrganizationProvider;
import com.uniccc.authentication.service.AuthenticationService;
import com.uniccc.authentication.service.JwtService;
import com.uniccc.common.core.entity.vo.ResultDto;
import com.uniccc.common.core.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

import static com.uniccc.common.core.enums.ResultCode.ORGAN_GATEWAY_ERROR;

@Slf4j
@RestController
@RequestMapping(value = "/api/authen")
public class AuthenticationController {



    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private OrganizationProvider organizationProvider;

    @Autowired
    private JwtService jwtService;


    @GetMapping(value = "/default/identity")
    public ResponseEntity<ResultDto> getIdentityDto(){
        String username = authenticationService.getCurrentUserName();
        String clientId = authenticationService.getClientId();
        return ResponseEntity.ok().body(ResultDto.success(jwtService.getIdentityDto(username,clientId)));
    }


    @GetMapping(value = "/identity/List")
    public ResponseEntity<ResultDto> getIdentityDtoList(){
        return Optional.ofNullable(organizationProvider.getIdentityDtoList(authenticationService.getCurrentUserName()))
                .filter(HttpEntity::hasBody)
                .map(HttpEntity::getBody)
                .map(ResultDto::success)
                .map(ResponseEntity::ok)
                .orElseThrow(()->new BaseException(ORGAN_GATEWAY_ERROR));
    }

    @PutMapping(value = "/identity")
    public ResponseEntity<ResultDto> setIdentityDefault(@Valid @RequestBody IdentityDto identityDto){
        String username = authenticationService.getCurrentUserName();
        String clientId = authenticationService.getClientId();
        return Optional.ofNullable(organizationProvider.setIdentityDefault(username,clientId,identityDto.getId()))
                .filter(HttpEntity::hasBody)
                .map(HttpEntity::getBody)
                .map(identity->{
                    jwtService.refreshJwt(username,clientId,identity);
                    return ResponseEntity.ok(ResultDto.success(true));
                })
              .orElseThrow(()->new BaseException(ORGAN_GATEWAY_ERROR));
    }

    @GetMapping(value = "/visit")
    public ResponseEntity<Boolean> isVisit(@RequestParam("url")String url,@RequestParam("method")String method, @RequestParam("ip") String ip){
        return Optional.ofNullable(organizationProvider.isVisit(authenticationService.getCurrentUserName(),url,method,authenticationService.getClientId(),ip))
                .filter(HttpEntity::hasBody)
                .map(HttpEntity::getBody)
                .map(ResponseEntity::ok)
                .orElseThrow(()->new BaseException(ORGAN_GATEWAY_ERROR));
    }

    @GetMapping(value = "/jwt")
    public ResponseEntity<TokenMsgDto> getJwt(@RequestParam(value = "identity",required = false)String identity){
        String username = authenticationService.getCurrentUserName();
        String clientId = authenticationService.getClientId();
        String token = jwtService.getJwt(username,clientId);
        return ResponseEntity.ok().body(jwtService.checkIdentity(token,identity,username,clientId));
    }
}
