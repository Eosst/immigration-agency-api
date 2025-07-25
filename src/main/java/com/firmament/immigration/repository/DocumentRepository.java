package com.firmament.immigration.repository;

import com.firmament.immigration.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    List<Document> findByAppointmentId(String appointmentId);
    void deleteByAppointmentId(String appointmentId);
}