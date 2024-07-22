package com.roamgram.travelDiary.application.service.wallet;

import com.roamgram.travelDiary.application.service.tags.TagsAccessService;
import com.roamgram.travelDiary.common.auth.service.AuthUserServiceImpl;
import com.roamgram.travelDiary.domain.model.tags.Tags;
import com.roamgram.travelDiary.domain.model.wallet.mapper.toentity.MonetaryEventEntityConverterFactory;
import com.roamgram.travelDiary.domain.model.wallet.mapper.toentity.MonetaryEventEntityConverterStrategy;
import com.roamgram.travelDiary.repository.persistence.user.UserProfileRepository;
import com.roamgram.travelDiary.repository.persistence.wallet.MonetaryEventEntityRepository;
import com.roamgram.travelDiary.domain.model.wallet.aggregate.MonetaryEvent;
import com.roamgram.travelDiary.domain.model.wallet.entity.MonetaryEventEntity;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.roamgram.travelDiary.domain.model.wallet.entity.EventType.*;

@Service
public class MonetaryDomainMutationService {

    private final MonetaryEventEntityRepository monetaryEventEntityRepository;
    private final TagsAccessService tagsAccessService;
    private final AuthUserServiceImpl authUserServiceImpl;

    @Autowired
    public MonetaryDomainMutationService(MonetaryEventEntityRepository monetaryEventEntityRepository, TagsAccessService tagsAccessService, UserProfileRepository userProfileRepository, AuthUserServiceImpl authUserServiceImpl) {
        this.monetaryEventEntityRepository = monetaryEventEntityRepository;
        this.tagsAccessService = tagsAccessService;
        this.authUserServiceImpl = authUserServiceImpl;
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
    public void delete(UUID transactionId) {
        List<MonetaryEventEntity> deletable = monetaryEventEntityRepository.findAllByMonetaryTransactionId(transactionId);
        monetaryEventEntityRepository.deleteAll(deletable);
    }

    //    only same type updates are possible
    @Transactional
    public List<MonetaryEventEntity> update(UUID transactionId, MonetaryEvent update) {
        List<MonetaryEventEntity> updateList = monetaryEventEntityRepository.findAllByMonetaryTransactionId(transactionId);
        List<MonetaryEventEntity> updateConverted = convertToEntity(update);
        if (updateList.size() != updateConverted.size()) {
            throw new RuntimeException("the entities created do not match in their specific types for update");
        }

        //may contain error where the desired side effects do not manifest
        IntStream.range(0, updateList.size())
                .forEach(idx -> {
                    MonetaryEventEntity original = updateList.get(idx);
                    MonetaryEventEntity updateEntity = updateConverted.get(idx);
                    original.setAmount(updateEntity.getAmount());
                    original.setCurrency(updateEntity.getCurrency());
                    original.setConversionRate(updateEntity.getConversionRate());
                    original.setDescription(updateEntity.getDescription());
                    original.setIsSource(updateEntity.getIsSource());
                    original.setTimestamp(updateEntity.getTimestamp());
                });
        return monetaryEventEntityRepository.saveAll(updateList);
    }

    @Transactional
    public List<MonetaryEventEntity> save(MonetaryEvent monetaryEvent) {
        List<MonetaryEventEntity> convertedList = convertToEntity(monetaryEvent);
        UUID userProfileId = authUserServiceImpl.getCurrentUser().getId();
        convertedList.forEach(mon->mon.setUserProfileId(userProfileId));
        return monetaryEventEntityRepository.saveAll(convertedList);
    }


    private List<MonetaryEventEntity> convertToEntity(MonetaryEvent monetaryEvent) {
        MonetaryEventEntityConverterStrategy strategy = MonetaryEventEntityConverterFactory.getStrategy(monetaryEvent);
        return strategy.convert(monetaryEvent);
    }


}
