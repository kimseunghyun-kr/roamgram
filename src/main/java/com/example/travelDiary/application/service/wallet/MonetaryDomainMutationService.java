package com.example.travelDiary.application.service.wallet;

import com.example.travelDiary.application.service.tags.TagsAccessService;
import com.example.travelDiary.domain.model.tags.Tags;
import com.example.travelDiary.domain.model.wallet.mapper.toentity.MonetaryEventEntityConverterFactory;
import com.example.travelDiary.domain.model.wallet.mapper.toentity.MonetaryEventEntityConverterStrategy;
import com.example.travelDiary.repository.persistence.wallet.MonetaryEventEntityRepository;
import com.example.travelDiary.domain.model.wallet.aggregate.MonetaryEvent;
import com.example.travelDiary.domain.model.wallet.entity.MonetaryEventEntity;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.example.travelDiary.domain.model.wallet.entity.EventType.*;

@Service
public class MonetaryDomainMutationService {

    private final MonetaryEventEntityRepository monetaryEventEntityRepository;
    private final TagsAccessService tagsAccessService;

    @Autowired
    public MonetaryDomainMutationService(MonetaryEventEntityRepository monetaryEventEntityRepository, TagsAccessService tagsAccessService) {
        this.monetaryEventEntityRepository = monetaryEventEntityRepository;
        this.tagsAccessService = tagsAccessService;
    }

    @Transactional
    public void addTag(UUID transactionId, Tags tag) {
        MonetaryEventEntity monetaryEventEntity = monetaryEventEntityRepository.findById(transactionId).orElseThrow();
        UUID monetaryEventEntityId = monetaryEventEntity.getId();
        switch (monetaryEventEntity.getEventType()) {
            case CURRENCY_CONVERSION:
                tagsAccessService.addTagToEntity(tag.getId(), CURRENCY_CONVERSION.name(), monetaryEventEntityId);
                break;
            case EXPENDITURE:
                tagsAccessService.addTagToEntity(tag.getId(), EXPENDITURE.name(), monetaryEventEntityId);
                break;
            case INCOME:
                tagsAccessService.addTagToEntity(tag.getId(), INCOME.name(), monetaryEventEntityId);
                break;
            default:
                // Handle default case if needed
                break;
        }
    }

    @Transactional
    public void deleteTags(UUID transactionId, Tags tag) {
        MonetaryEventEntity monetaryEventEntity = monetaryEventEntityRepository.findById(transactionId).orElseThrow();
        UUID monetaryEventEntityId = monetaryEventEntity.getId();
        tagsAccessService.deleteTagFromEntity(tag.getId(), monetaryEventEntityId);
    }

    @Transactional
    public void delete(String transactionId) {
        List<MonetaryEventEntity> deletable = monetaryEventEntityRepository.findAllByTransactionId(transactionId);
        monetaryEventEntityRepository.deleteAll(deletable);
    }

    //    only same type updates are possible
    @Transactional
    public List<MonetaryEventEntity> update(String transactionId, MonetaryEvent update) {
        List<MonetaryEventEntity> updateList = monetaryEventEntityRepository.findAllByTransactionId(transactionId);
        List<MonetaryEventEntity> updateConverted = convertToEntity(update);
        if (updateList.size() != updateConverted.size()) {
            throw new RuntimeException("the entities created do not match for update");
        }

        //may contain error where the desired side effects do not manifest
        IntStream.range(0, updateList.size())
                .peek(idx -> {
                    MonetaryEventEntity original = updateList.get(idx);
                    MonetaryEventEntity updateEntity = updateConverted.get(idx);
                    original.setAmount(updateEntity.getAmount());
                    original.setCurrency(updateEntity.getCurrency());
                    original.setConversionRate(updateEntity.getConversionRate());
                    original.setDescription(updateEntity.getDescription());
                    original.setSource(updateEntity.getSource());
                    original.setTimestamp(updateEntity.getTimestamp());
                });
        return monetaryEventEntityRepository.saveAll(updateList);
    }

    @Transactional
    public List<MonetaryEventEntity> save(MonetaryEvent monetaryEvent) {
        List<MonetaryEventEntity> convertedList = convertToEntity(monetaryEvent);
        return monetaryEventEntityRepository.saveAll(convertedList);
    }


    private List<MonetaryEventEntity> convertToEntity(MonetaryEvent monetaryEvent) {
        MonetaryEventEntityConverterStrategy strategy = MonetaryEventEntityConverterFactory.getStrategy(monetaryEvent);
        return strategy.convert(monetaryEvent);
    }


}
