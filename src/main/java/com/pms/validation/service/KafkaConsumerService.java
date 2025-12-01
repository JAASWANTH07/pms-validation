package com.pms.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.validation.dto.IngestionEventDto;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationOutputDto;
import com.pms.validation.dto.ValidationResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class KafkaConsumerService {

    private static final Logger logger = Logger.getLogger(KafkaConsumerService.class.getName());

    @Autowired
    private com.pms.validation.service.TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxService outboxService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private KafkaTemplate<String, ValidationOutputDto> kafkaTemplate;

    @Autowired
    private ObjectMapper mapper;

    @KafkaListener(topics = "ingestion-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void processIngestionMessage(String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            IngestionEventDto ingestionEvent = mapper.readValue(payload, IngestionEventDto.class);

            TradeDto trade = mapper.readValue(ingestionEvent.getPayloadBytes(), TradeDto.class);

            // idempotency based on tradeId inside payload
            if (idempotencyService.isAlreadyProcessed(trade.getTradeId())) {
                logger.info("Ignoring duplicate trade: " + trade.getTradeId());
                return;
            }

            logger.info("Processing trade from ingestion: " + trade.getTradeId());

            if (!idempotencyService.markAsProcessed(trade.getTradeId(), "ingestion-topic")) {
                logger.warning(
                        "Failed to mark trade as processed (possible duplicate): " + trade.getTradeId());
                return;
            }

            ValidationResultDto result = tradeValidationService.validateTrade(trade);

            ValidationOutputDto validationEvent = outboxService.buildValidationEvent(trade, result);

            String topic = result.isValid() ? "validation-topic" : "validation-dlq";
            outboxService.saveValidationEvent(trade, result, result.isValid() ? "SUCCESS" : "FAILED");

            kafkaTemplate.send(topic, validationEvent.getTradeId().toString(), validationEvent);
            logger.info("Sent validation event to " + topic + " for trade: " + trade.getTradeId());

        } catch (Exception ex) {
            logger.severe("Error processing ingestion message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
