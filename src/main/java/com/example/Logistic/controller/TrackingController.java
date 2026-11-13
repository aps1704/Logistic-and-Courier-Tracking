package com.example.Logistic.controller;

import com.example.Logistic.entity.Parcel;
import com.example.Logistic.entity.TrackingEvent;
import com.example.Logistic.repository.ParcelRepository;
import com.example.Logistic.repository.TrackingEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    private final ParcelRepository parcels;
    private final TrackingEventRepository events;

    public TrackingController(
            ParcelRepository parcels,
            TrackingEventRepository events) {
        this.parcels = parcels;
        this.events = events;
    }

    @GetMapping("/{trackingId}")
    public ResponseEntity<?> track(
            @PathVariable String trackingId) {

        return parcels.findByTrackingId(trackingId)
                .map(parcel -> {
                    List<TrackingEvent> history =
                            events.findByParcelTrackingIdOrderByEventTimeDesc(
                                    trackingId);

                    List<Map<String, Object>> timeline = history.stream()
                            .map(event -> Map.<String, Object>of(
                                    "status", event.getStatus(),
                                    "location", event.getLocation() == null
                                            ? "" : event.getLocation(),
                                    "description", event.getDescription() == null
                                            ? "" : event.getDescription(),
                                    "eventTime", event.getEventTime()
                            ))
                            .toList();

                    return ResponseEntity.ok(Map.of(
                            "trackingId", parcel.getTrackingId(),
                            "status", parcel.getStatus(),
                            "history", timeline
                    ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}