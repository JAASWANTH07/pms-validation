package com.pms.validation.service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.pool2.ObjectPool;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeValidationService {

    private final ObjectPool<KieSession> kieSessionPool;

    public TradeValidationService(ObjectPool<KieSession> kieSessionPool) {
        this.kieSessionPool = kieSessionPool;
    }

    public ValidationResultDto validateTrade(TradeDto trade) {
        KieSession kieSession = null;
        try {
            kieSession = kieSessionPool.borrowObject();
            ValidationResultDto result = new ValidationResultDto();

            kieSession.insert(trade);
            kieSession.insert(result);
            kieSession.fireAllRules();

            kieSession.getFactHandles().forEach(kieSession::delete);

            return result;
        } catch (RuntimeException ex) {
            log.error("Runtime error during trade validation: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error during trade validation: {}", ex.getMessage());
            throw new RuntimeException("Validation error", ex);
        } finally {
            if (kieSession != null) {
                try {
                    kieSessionPool.returnObject(kieSession);
                } catch (Exception e) {
                    log.warn("Failed returning KieSession to pool: {}", e.getMessage());
                }
            }
        }
    }
}
