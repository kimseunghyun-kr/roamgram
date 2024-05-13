package com.example.travelDiary.domain.model.travel;

import com.example.travelDiary.domain.model.location.Place;
import com.example.travelDiary.domain.model.review.Review;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Data
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID TravelPlanId;

    @OneToOne
    @Cascade(CascadeType.ALL)
    public Place place;

    @OneToOne
    @Cascade(CascadeType.ALL)
    public Review review;

    public Boolean isActuallyVisited;

    public LocalDate travelDate;

    public Integer orderOfTravel;

    public LocalTime travelStartTimeEstimate;

    public LocalTime travelDepartTimeEstimate;

//    public Set<UserTags> travelCategory;


}
