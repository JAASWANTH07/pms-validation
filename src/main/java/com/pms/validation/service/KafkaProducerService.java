package com.pms.validation.service;

import com.pms.validation.entity.ValidationOutboxEntity;
import com.pms.validation.mapper.ProtoEntityMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.pms.validation.proto.TradeEventProto;

@Service
public class KafkaProducerService{

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Autowired
    private ProtoEntityMapper protoEntityMapper;

    private static final String topic = "validation-topic";

    public void sendValidationEvent(ValidationOutboxEntity event) throws Exception {

        TradeEventProto protoEvent = protoEntityMapper.toProto(event);

        byte[] eventBytes = protoEvent.toByteArray();

        kafkaTemplate.send(topic, protoEvent.getPortfolioId(), eventBytes).get();
    }
}
