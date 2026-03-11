package com.stableflow.system.security;

import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentMerchantProvider {

    public CurrentMerchant requireCurrentMerchant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentMerchant currentMerchant)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentMerchant;
    }

    public Long requireCurrentMerchantId() {
        return requireCurrentMerchant().merchantId();
    }
}
