package com.pms.validation.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;
import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.proto.TradeEventProto;

@Component
public class ProtoEntityMapper {
     // --------------------- ENTITY → PROTO ---------------------
    public static TradeEventProto toProto(ValidationOutboxEntity entity) {

        return TradeEventProto.newBuilder()
                .setPortfolioId(entity.getPortfolioId().toString())
                .setTradeId(entity.getTradeId().toString())
                .setSymbol(entity.getSymbol())
                .setSide(entity.getSide().name())
                .setPricePerStock(entity.getPricePerStock().doubleValue())
                .setQuantity(entity.getQuantity())
                .setTimestamp(convertLocalDateTime(entity.getTradeTimestamp()))
                .build();
    }

    // --------------------- PROTO → ENTITY (optional) ---------------------
    public static ValidationOutboxEntity toEntity(TradeEventProto proto) {

        return ValidationOutboxEntity.builder()
                .portfolioId(UUID.fromString(proto.getPortfolioId()))
                .tradeId(UUID.fromString(proto.getTradeId()))
                .symbol(proto.getSymbol())
                .side(Enum.valueOf(com.pms.validation.enums.TradeSide.class, proto.getSide()))
                .pricePerStock(java.math.BigDecimal.valueOf(proto.getPricePerStock()))
                .quantity(proto.getQuantity())
                .tradeTimestamp(convertTimestamp(proto.getTimestamp()))
                .build();
    }

    // --------------------- Timestamp Converters ---------------------
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
