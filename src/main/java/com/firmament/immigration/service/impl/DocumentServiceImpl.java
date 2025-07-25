package com.firmament.immigration.service.impl;

import com.firmament.immigration.dto.response.DocumentResponse;
import com.firmament.immigration.entity.Appointment;
import com.firmament.immigration.entity.Document;
import com.firmament.immigration.exception.BusinessException;
import com.firmament.immigration.exception.ResourceNotFoundException;
import com.firmament.immigration.repository.AppointmentRepository;
import com.firmament.immigration.repository.DocumentRepository;
import com.firmament.immigration.service.DocumentService;
import com.firmament.immigration.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    @Value("${app.upload.path}")
    private String uploadPath;

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "doc", "docx", "jpg", "jpeg", "png", "gif"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public List<DocumentResponse> uploadDocuments(String appointmentId, List<MultipartFile> files) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        List<Document> documents = new ArrayList<>();
        List<String> uploadedFileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFile(file);

            try {
                String fileName = generateFileName(file.getOriginalFilename());
                Path filePath = Paths.get(uploadPath, appointmentId, fileName);

                // Create directories if they don't exist
                Files.createDirectories(filePath.getParent());

                // Save file
                Files.write(filePath, file.getBytes());

                // Create document entity
                Document document = Document.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .filePath(filePath.toString())
                        .fileSize(file.getSize())
                        .appointment(appointment)
                        .build();

                documents.add(documentRepository.save(document));
                uploadedFileNames.add(file.getOriginalFilename());

                log.info("Document uploaded: {} for appointment: {}", fileName, appointmentId);

            } catch (IOException e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                throw new BusinessException("Failed to upload file: " + file.getOriginalFilename());
            }
        }

        // Send confirmation email
        if (!uploadedFileNames.isEmpty()) {
            emailService.sendDocumentUploadConfirmation(appointment, uploadedFileNames);
        }

        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        return mapToResponse(document);
    }

    @Override
    public void deleteDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        try {
            // Delete file from disk
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);

            // Delete from database
            documentRepository.delete(document);

            log.info("Document deleted: {}", documentId);

        } catch (IOException e) {
            log.error("Failed to delete file: {}", document.getFilePath(), e);
            throw new BusinessException("Failed to delete document");
        }
    }

    @Override
    public List<DocumentResponse> getAppointmentDocuments(String appointmentId) {
        List<Document> documents = documentRepository.findByAppointmentId(appointmentId);

        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] downloadDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        try {
            Path filePath = Paths.get(document.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read file: {}", document.getFilePath(), e);
            throw new BusinessException("Failed to download document");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds maximum allowed size of 10MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("File type not allowed. Allowed types: " +
                    String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private DocumentResponse mapToResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setUploadedAt(document.getCreatedAt());
        return response;
    }
}