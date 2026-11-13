package com.example.Logistic.controller;

import com.example.Logistic.entity.Parcel;
import com.example.Logistic.entity.UserAccount;
import com.example.Logistic.repository.ParcelRepository;
import com.example.Logistic.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final ParcelRepository parcels;
    private final UserAccountRepository users;

    public CustomerController(
            ParcelRepository parcels,
            UserAccountRepository users) {
        this.parcels = parcels;
        this.users = users;
    }

    @GetMapping("/parcels")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> myParcels(
            Authentication authentication) {

        UserAccount user = getUser(authentication);

        return parcels.findByCustomerId(user.getId())
                .stream()
                .map(this::parcelResponse)
                .toList();
    }

    @PostMapping("/parcels")
    @Transactional
    public Map<String, Object> createParcel(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        UserAccount user = getUser(authentication);

        String sender = value(body, "sender");
        String receiver = value(body, "receiver");
        String senderPhone = value(body, "senderPhone");
        String receiverPhone = value(body, "receiverPhone");
        String pickupAddress = value(body, "pickupAddress");
        String deliveryAddress = value(body, "deliveryAddress");

        double weight;

        try {
            weight = Double.parseDouble(
                    String.valueOf(body.get("weight")));
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enter a valid parcel weight.");
        }

        if (sender.isBlank() || receiver.isBlank()
                || senderPhone.isBlank() || receiverPhone.isBlank()
                || pickupAddress.isBlank() || deliveryAddress.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Please complete all parcel fields.");
        }

        if (!senderPhone.matches("[0-9]{10}")
                || !receiverPhone.matches("[0-9]{10}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Enter valid 10-digit phone numbers.");
        }

        if (!Double.isFinite(weight) || weight <= 0 || weight > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Weight must be greater than 0 and at most 1000 kg.");
        }

        Parcel parcel = new Parcel();
        parcel.setTrackingId("CR" +
                UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12).toUpperCase());
        parcel.setSender(sender);
        parcel.setReceiver(receiver);
        parcel.setSenderPhone(senderPhone);
        parcel.setReceiverPhone(receiverPhone);
        parcel.setPickupAddress(pickupAddress);
        parcel.setDeliveryAddress(deliveryAddress);
        parcel.setWeight(weight);
        parcel.setStatus("Booked");
        parcel.setCustomer(user);

        parcels.save(parcel);

        return Map.of(
                "message", "Parcel order placed successfully.",
                "trackingId", parcel.getTrackingId()
        );
    }

    @PostMapping("/parcels/{id}/cancel")
    @Transactional
    public Map<String, String> cancelParcel(
            @PathVariable Long id,
            Authentication authentication) {

        UserAccount user = getUser(authentication);

        Parcel parcel = parcels.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Parcel not found."));

        if (parcel.getCustomer() == null
                || !parcel.getCustomer().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Parcel not found.");
        }

        if (!"Booked".equalsIgnoreCase(parcel.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only booked parcels can be cancelled.");
        }

        parcel.setStatus("Cancelled");
        parcels.save(parcel);

        return Map.of(
                "message", "Parcel cancelled successfully.",
                "trackingId", parcel.getTrackingId(),
                "status", parcel.getStatus()
        );
    }

    private UserAccount getUser(Authentication authentication) {
        return users.findByEmail(
                authentication.getName().trim().toLowerCase()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Please log in again."));
    }

    private String value(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private Map<String, Object> parcelResponse(Parcel parcel) {
        return Map.of(
                "id", parcel.getId(),
                "trackingId", parcel.getTrackingId(),
                "sender", parcel.getSender() == null ? "" : parcel.getSender(),
                "receiver", parcel.getReceiver() == null ? "" : parcel.getReceiver(),
                "status", parcel.getStatus() == null ? "Booked" : parcel.getStatus(),
                "weight", parcel.getWeight() == null ? 0.0 : parcel.getWeight()
        );
    }
}