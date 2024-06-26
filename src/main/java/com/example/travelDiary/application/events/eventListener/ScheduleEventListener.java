package com.example.travelDiary.application.events.eventListener;

import com.example.travelDiary.application.events.travel.ScheduleCreatedEvent;
import com.example.travelDiary.application.events.travel.ScheduleDeletedEvent;
import com.example.travelDiary.application.events.travel.SchedulePreDeletedEvent;
import com.example.travelDiary.application.events.travel.ScheduleUpdatedEvent;
import com.example.travelDiary.application.service.location.PlaceMutationService;
import com.example.travelDiary.common.permissions.domain.Resource;
import com.example.travelDiary.common.permissions.service.ResourceService;
import com.example.travelDiary.domain.model.location.Place;
import com.example.travelDiary.domain.model.travel.Schedule;
import com.example.travelDiary.domain.model.travel.TravelPlan;
import com.example.travelDiary.repository.persistence.travel.ScheduleRepository;
import com.example.travelDiary.repository.persistence.travel.TravelPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class ScheduleEventListener {

    private final ScheduleRepository scheduleRepository;
    private final PlaceMutationService placeMutationService;
    private final TravelPlanRepository travelPlanRepository;
    private final ResourceService resourceService;

    @Autowired
    public ScheduleEventListener(ScheduleRepository scheduleRepository,
                                 PlaceMutationService placeMutationService,
                                 TravelPlanRepository travelPlanRepository,
                                 ResourceService resourceService) {
        this.scheduleRepository = scheduleRepository;
        this.placeMutationService = placeMutationService;
        this.travelPlanRepository = travelPlanRepository;
        this.resourceService = resourceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleScheduleCreatedEvent(ScheduleCreatedEvent event) {
        Schedule schedule = event.getSchedule();
        schedule = scheduleRepository.findById(schedule.getId()).orElseThrow();

        TravelPlan travelPlan = travelPlanRepository.findById(event.getTravelPlanId())
                .orElseThrow(()-> new IllegalArgumentException("schedule is not under any valid travelPlan"));
        List<Schedule> travelPlanSchedule = travelPlan.getScheduleList();
        travelPlanSchedule.add(schedule);
        travelPlanRepository.save(travelPlan);

        if(schedule.getPlace() != null) {
            log.info("error resulted : {}", schedule.getPlace());
        }

        Place place = placeMutationService.createPlace(event.getPlace());
        schedule.setPlace(place);

        Resource resource = resourceService.createResource(schedule, "private");
        schedule.setResource(resource);
        scheduleRepository.save(schedule);
        scheduleRepository.flush();

        // Additional logic if needed for Schedule creation
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSchedulePreDeletedEvent(SchedulePreDeletedEvent event) {
        log.info("prehandler is published");
        resourceService.delinkPermissions(List.of(event.getScheduleId()));
        TravelPlan travelPlan = travelPlanRepository.findById(event.getTravelPlanId()).orElseThrow();
        travelPlan.getScheduleList().remove(scheduleRepository.findById(event.getScheduleId()).orElseThrow());
        travelPlanRepository.save(travelPlan);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleScheduleDeletedEvent(ScheduleDeletedEvent event) {
        log.info("postHandler is published");
        UUID placeId = event.getPlaceId();
        if(placeId == null) {
            return;
        }
        List<Schedule> schedule = scheduleRepository.findByPlaceId(placeId);
        log.info("schedule is {} " , schedule);
        if (scheduleRepository.findByPlaceId(placeId).isEmpty()) {
            placeMutationService.deletePlace(placeId);
        }
        scheduleRepository.flush();
    }

    @EventListener
    public void handleScheduleUpdatedEvent(ScheduleUpdatedEvent event) {
        // Additional logic if needed for Schedule update
    }


}

