package com.pms.validation.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;
import com.pms.validation.dto.TradeDto;
import com.pms.validation.enums.TradeSide;
import com.pms.validation.proto.TradeEventProto;

@Component
public class ProtoDTOMapper {
    // ---------- PROTO -> DTO ----------
    public static TradeDto toDto(TradeEventProto proto) {
        return TradeDto.builder()
                .tradeId(UUID.fromString(proto.getTradeId()))
                .portfolioId(UUID.fromString(proto.getPortfolioId()))
                .symbol(proto.getSymbol())
                .side(TradeSide.valueOf(proto.getSide()))
                .pricePerStock(java.math.BigDecimal.valueOf(proto.getPricePerStock()))
                .quantity(proto.getQuantity())
                .timestamp(convertTimestamp(proto.getTimestamp()))
                .build();
    }

    // ---------- DTO -> PROTO ----------
    public static TradeEventProto toProto(TradeDto dto) {
        return TradeEventProto.newBuilder()
                .setTradeId(dto.getTradeId().toString())
                .setPortfolioId(dto.getPortfolioId().toString())
                .setSymbol(dto.getSymbol())
                .setSide(dto.getSide().name())
                .setPricePerStock(dto.getPricePerStock().doubleValue())
                .setQuantity(dto.getQuantity())
                .setTimestamp(convertLocalDateTime(dto.getTimestamp()))
                .build();
    }

    // ---------- Converters ----------
    private static LocalDateTime convertTimestamp(Timestamp ts) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()),
                ZoneId.systemDefault()
        );
    }

    private static Timestamp convertLocalDateTime(LocalDateTime ldt) {
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
