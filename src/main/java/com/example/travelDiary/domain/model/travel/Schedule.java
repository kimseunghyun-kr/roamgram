package com.example.travelDiary.domain.model.travel;

import com.example.travelDiary.domain.model.location.Place;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Data
public class Schedule {
    @Id
    private Long id;

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
