package com.firmament.immigration.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;
    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx", "jpg", "jpeg", "png", "gif");
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
                String resourceType = getResourceType(file.getOriginalFilename());

                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "resource_type", resourceType,
                        "folder", "immigration_documents/" + appointmentId,
                        // NEW: Tell Cloudinary to use the original filename as the public ID basis
                        "use_filename", true,
                        "unique_filename", false
                ));

                String fileUrl = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");

                // --- CRITICAL CHANGE: Modify the URL for non-image files ---
                if ("raw".equals(resourceType)) {
                    // This injects a special flag into the URL.
                    // "fl_attachment" tells Cloudinary to send headers that force a download
                    // with the original filename.
                    fileUrl = fileUrl.replace("/upload/", "/upload/fl_attachment/");
                }

                Document document = Document.builder()
                        .fileName(file.getOriginalFilename())
                        .fileType(file.getContentType())
                        .filePath(fileUrl)
                        .publicId(publicId)
                        .fileSize(file.getSize())
                        .appointment(appointment)
                        .build();

                documents.add(documentRepository.save(document));
                uploadedFileNames.add(file.getOriginalFilename());

                log.info("Document uploaded to Cloudinary: {} for appointment: {}", fileUrl, appointmentId);

            } catch (IOException e) {
                log.error("Failed to upload file to Cloudinary: {}", file.getOriginalFilename(), e);
                throw new BusinessException("Failed to upload file: " + file.getOriginalFilename());
            }
        }

        if (!uploadedFileNames.isEmpty()) {
            emailService.sendDocumentUploadConfirmation(appointment, uploadedFileNames);
        }

        return documents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // --- NO CHANGES BELOW THIS LINE ---

    private String getResourceType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        if (List.of("jpg", "jpeg", "png", "gif").contains(extension)) {
            return "image";
        }
        return "raw";
    }

    @Override
    public void deleteDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        try {
            String resourceType = getResourceType(document.getFileName());
            log.info("Attempting to delete file from Cloudinary with public_id: {} and resource_type: {}", document.getPublicId(), resourceType);
            cloudinary.uploader().destroy(document.getPublicId(), ObjectUtils.asMap("resource_type", resourceType));
            log.info("Successfully deleted file from Cloudinary.");
            documentRepository.delete(document);
            log.info("Successfully deleted document record from database: {}", documentId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary for documentId: {}", documentId, e);
            throw new BusinessException("Error deleting document. Please try again.");
        }
    }

    @Override
    public byte[] downloadDocument(String documentId) {
        throw new UnsupportedOperationException("Download is handled via URL on the frontend.");
    }

    @Override
    public DocumentResponse getDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return mapToResponse(document);
    }

    @Override
    public List<DocumentResponse> getAppointmentDocuments(String appointmentId) {
        List<Document> documents = documentRepository.findByAppointmentId(appointmentId);
        return documents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException("File size exceeds 10MB");
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private DocumentResponse mapToResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setUploadedAt(document.getCreatedAt());
        response.setUrl(document.getFilePath());
        return response;
    }
}