package com.example.Logistic.controller;

import com.example.Logistic.entity.Parcel;
import com.example.Logistic.entity.TrackingEvent;
import com.example.Logistic.repository.ParcelRepository;
import com.example.Logistic.repository.TrackingEventRepository;
import com.example.Logistic.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ParcelRepository parcels;
    private final TrackingEventRepository events;
    private final UserAccountRepository users;

    public AdminController(
            ParcelRepository parcels,
            TrackingEventRepository events,
            UserAccountRepository users) {
        this.parcels = parcels;
        this.events = events;
        this.users = users;
    }
    @Transactional(readOnly = true)
    @GetMapping("/parcels")
    public List<Map<String, Object>> allParcels(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {


        return parcels.findAll().stream()
                .filter(p -> search == null || search.isBlank()
                        || p.getTrackingId().toLowerCase()
                        .contains(search.trim().toLowerCase())
                        || (p.getReceiver() != null && p.getReceiver()
                        .toLowerCase().contains(search.trim().toLowerCase())))
                .filter(p -> status == null || status.isBlank()
                        || p.getStatus().equalsIgnoreCase(status))
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "trackingId", p.getTrackingId(),
                        "sender", p.getSender() == null ? "" : p.getSender(),
                        "receiver", p.getReceiver() == null ? "" : p.getReceiver(),
                        "status", p.getStatus(),
                        "customerEmail", p.getCustomer() == null
                                ? "" : p.getCustomer().getEmail()
                ))
                .toList();
    }

    @PostMapping("/parcels")
    @Transactional
    public ResponseEntity<?> createParcel(
            @RequestBody Map<String, String> body) {

        String trackingId = body.getOrDefault("trackingId", "").trim();
        String email = body.getOrDefault("customerEmail", "")
                .trim().toLowerCase();

        if (trackingId.isBlank() || email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Tracking ID and customer email are required."));
        }

        if (parcels.findByTrackingId(trackingId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Tracking ID already exists."));
        }

        var customer = users.findByEmail(email);

        if (customer.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Register the customer before assigning a parcel."));
        }

        Parcel parcel = new Parcel();
        parcel.setTrackingId(trackingId);
        parcel.setSender(body.getOrDefault("sender", ""));
        parcel.setReceiver(body.getOrDefault("receiver", ""));
        parcel.setStatus("Booked");
        parcel.setCustomer(customer.get());

        parcels.save(parcel);

        addEvent(parcel, "Booked", "Parcel booked");

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Parcel created", "trackingId", trackingId));
    }

    @PutMapping("/parcels/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.getOrDefault("status", "").trim();
        String location = body.getOrDefault("location", "").trim();
        String description = body.getOrDefault("description", "").trim();

        List<String> allowed = List.of(
                "Booked", "In Transit", "Out for Delivery", "Delivered",
                "Delayed", "Cancelled"
        );

        if (!allowed.contains(status)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid delivery status."));
        }

        return parcels.findById(id)
                .map(parcel -> {
                    parcel.setStatus(status);
                    parcels.save(parcel);
                    addEvent(parcel, status, location, description);

                    return ResponseEntity.ok(
                            Map.of("message", "Parcel status updated."));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/parcels/{id}")
    @Transactional
    public ResponseEntity<?> deleteParcel(@PathVariable Long id) {
        return parcels.findById(id)
                .map(parcel -> {
                    events.deleteAll(
                            events.findByParcelTrackingIdOrderByEventTimeDesc(
                                    parcel.getTrackingId()));
                    parcels.delete(parcel);
                    return ResponseEntity.noContent().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void addEvent(Parcel parcel, String status, String description) {
        addEvent(parcel, status, "", description);
    }

    private void addEvent(
            Parcel parcel, String status,
            String location, String description) {

        TrackingEvent event = new TrackingEvent();
        event.setParcel(parcel);
        event.setStatus(status);
        event.setLocation(location);
        event.setDescription(description);
        events.save(event);
    }
}