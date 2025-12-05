package com.pms.validation.service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.dto.ValidationResultDto;
import com.pms.validation.entity.InvalidTradeEntity;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.exception.NonRetryableException;
import com.pms.validation.exception.RetryableException;
import com.pms.validation.repository.InvalidTradeRepository;
import com.pms.validation.repository.ValidationOutboxRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ValidationCore {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private TradeValidationService tradeValidationService;

    @Autowired
    private ValidationOutboxRepository outboxRepo;

    @Autowired
    private InvalidTradeRepository invalidTradeRepo;

    // Atomic DB transaction: idempotency table insert + validate + outbox write.
    @Transactional
    public void handleTransaction(TradeDto trade) {
        try {
           
            idempotencyService.markAsProcessed(trade.getTradeId(), "ingestion-topic");

            ValidationResultDto result = tradeValidationService.validateTrade(trade);

            String status = result.isValid() ? "VALID" : "INVALID";
            String errors = result.getErrors().isEmpty() ? null
                    : result.getErrors().stream().collect(Collectors.joining("; "));

            if (status.equals("VALID")) {
                log.info("Trade {} is valid.", trade.getTradeId());

                ValidationOutboxEntity outbox = ValidationOutboxEntity.builder()
                        .eventId(UUID.randomUUID())
                        .tradeId(trade.getTradeId())
                        .portfolioId(trade.getPortfolioId())
                        .symbol(trade.getSymbol())
                        .side(trade.getSide())
                        .pricePerStock(trade.getPricePerStock())
                        .quantity(trade.getQuantity())
                        .tradeTimestamp(trade.getTimestamp())
                        .sentStatus("PENDING") // Outbox message Status
                        .validationStatus(status)
                        .validationErrors(null)
                        .build();

                outboxRepo.save(outbox);
            } else {
                log.info("Trade {} is invalid: {}", trade.getTradeId(), errors);

                InvalidTradeEntity invalidTrade = InvalidTradeEntity.builder()
                        .eventId(UUID.randomUUID())
                        .tradeId(trade.getTradeId())
                        .portfolioId(trade.getPortfolioId())
                        .symbol(trade.getSymbol())
                        .side(trade.getSide())
                        .pricePerStock(trade.getPricePerStock())
                        .quantity(trade.getQuantity())
                        .tradeTimestamp(trade.getTimestamp())
                        .sentStatus("PENDING")
                        .validationStatus(status)
                        .validationErrors(errors)
                        .build();

                invalidTradeRepo.save(invalidTrade);
            }

            log.info("Outbox entry inserted for trade {}", trade.getTradeId());

        } catch (RuntimeException ex) {
            log.error("Transaction failed for trade {}: {}", trade.getTradeId(), ex.getMessage());
            throw ex; // Trigger transaction rollback
        } catch (Exception ex) {
            log.error("Unexpected error for trade {}: {}", trade.getTradeId(), ex.getMessage());
            throw ex; // Do not rollback
        }
    }

    public void processTrade(TradeDto trade) {
        try {

            if (idempotencyService.isAlreadyProcessed(trade.getTradeId())) {
                log.info("Ignoring duplicate trade: " + trade.getTradeId());
                return;
            }

            log.info("Delegating trade " + trade.getTradeId() + " to processor");

            handleTransaction(trade);

        } catch(RuntimeException ex) {
            log.error("Transaction failed in IngestionProcessor.process: {}", ex.getMessage(), ex);
            throw new RetryableException(ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error in IngestionProcessor.process: {}", ex.getMessage(), ex);
            throw new NonRetryableException(ex.getMessage(), ex);
        }
    }
}
