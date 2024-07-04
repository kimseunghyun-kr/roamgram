package com.example.travelDiary.domain.model.wallet.aggregate;

import com.example.travelDiary.domain.model.wallet.Amount;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@JsonTypeName("expenditure")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expenditure implements MonetaryEvent {
    private UUID id;
    private Amount amount;
    private Currency currency;
    private String description;
    private Instant timestamp;

}
