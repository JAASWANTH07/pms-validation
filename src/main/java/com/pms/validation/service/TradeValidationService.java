package com.pms.validation.service;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.internal.util.logging.Log;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeValidationService {

    private final KieContainer kieContainer;

    public TradeValidationService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public ValidationResultDto validateTrade(TradeDto trade) {
        try {
            ValidationResultDto result = new ValidationResultDto();

            KieSession kieSession = kieContainer.newKieSession();

            try {
                kieSession.insert(trade);
                kieSession.insert(result);
                kieSession.fireAllRules();
            } finally {
                kieSession.dispose();
            }

            return result;
        } catch (RuntimeException ex) {
            log.error("Runtime error during trade validation: {}", ex.getMessage());
            throw ex;
        }
        catch (Exception ex) {
            log.error("Error during trade validation: {}", ex.getMessage());
            throw new RuntimeException("Validation error", ex);
        }
    }
}
