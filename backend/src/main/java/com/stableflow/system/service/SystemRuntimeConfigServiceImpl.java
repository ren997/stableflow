package com.stableflow.system.service;

import com.stableflow.system.config.SolanaProperties;
import com.stableflow.system.vo.SystemRuntimeConfigVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemRuntimeConfigServiceImpl implements SystemRuntimeConfigService {

    private final SolanaProperties solanaProperties;

    @Override
    public SystemRuntimeConfigVo getRuntimeConfig() {
        return new SystemRuntimeConfigVo(
            solanaProperties.resolvedNetwork(),
            solanaProperties.resolvedUsdcMintAddress(),
            solanaProperties.resolvedExplorerTxBaseUrl()
        );
    }
}
