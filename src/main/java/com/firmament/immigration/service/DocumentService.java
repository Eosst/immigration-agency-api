package com.firmament.immigration.service;

import com.firmament.immigration.dto.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DocumentService {
    List<DocumentResponse> uploadDocuments(String appointmentId, List<MultipartFile> files);
    DocumentResponse getDocument(String documentId);
    void deleteDocument(String documentId);
    List<DocumentResponse> getAppointmentDocuments(String appointmentId);
    byte[] downloadDocument(String documentId);
}