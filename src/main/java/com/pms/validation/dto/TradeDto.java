package com.pms.validation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.pms.validation.enums.TradeSide;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDto {
    private UUID tradeId;
    private UUID portfolioId;
    private String symbol;
    private TradeSide side;
    private BigDecimal pricePerStock;
    private Long quantity;
    private LocalDateTime timestamp;
}
