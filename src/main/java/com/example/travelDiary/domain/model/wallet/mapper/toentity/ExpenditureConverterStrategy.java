package com.example.travelDiary.domain.model.wallet.mapper.toentity;

import com.example.travelDiary.domain.model.wallet.aggregate.Expenditure;
import com.example.travelDiary.domain.model.wallet.aggregate.MonetaryEvent;
import com.example.travelDiary.domain.model.wallet.entity.EventType;
import com.example.travelDiary.domain.model.wallet.entity.MonetaryEventEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ExpenditureConverterStrategy implements MonetaryEventEntityConverterStrategy {

    @Override
    public List<MonetaryEventEntity> convert(MonetaryEvent monetaryEvent) {
        Expenditure expenditure = (Expenditure) monetaryEvent;
        return List.of(
                MonetaryEventEntity
                        .builder()
                        .transactionId(UUID.randomUUID().toString())
                        .source("source")
                        .amount(expenditure.getAmount())
                        .currency(expenditure.getCurrency())
                        .timestamp(Instant.now())
                        .description(expenditure.getDescription())
                        .eventType(EventType.EXPENDITURE)
                        .build()
        );
    }
}
