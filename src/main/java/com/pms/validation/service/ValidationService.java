package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.OutboxEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResult;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final KieContainer kieContainer;

    public ValidationService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public ValidationResult validateOutbox(OutboxEventDto outbox) {
        try {
            TradeDto trade = mapper.readValue(outbox.getPayloadBytes(), TradeDto.class);

            ValidationResult result = new ValidationResult();

            KieSession kieSession = kieContainer.newKieSession();

            try {
                kieSession.insert(trade);
                kieSession.insert(result);
                kieSession.fireAllRules();
            } finally {
                kieSession.dispose();
            }

            return result;
        } catch (Exception ex) {
            ValidationResult err = new ValidationResult();
            err.addError("Rule validation error: " + ex.getMessage());
            return err;
        }
    }
}
