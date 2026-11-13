package com.example.Logistic.repository;

import com.example.Logistic.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventRepository
        extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findByParcelTrackingIdOrderByEventTimeDesc(
            String trackingId
    );
}