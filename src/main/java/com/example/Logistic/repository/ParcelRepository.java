package com.example.Logistic.repository;

import com.example.Logistic.entity.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    Optional<Parcel> findByTrackingId(String trackingId);

    List<Parcel> findByCustomerId(Long customerId);

    List<Parcel> findByStatusIgnoreCase(String status);
}