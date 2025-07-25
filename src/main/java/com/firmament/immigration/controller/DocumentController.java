package com.firmament.immigration.controller;

import com.firmament.immigration.dto.response.DocumentResponse;
import com.firmament.immigration.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management endpoints")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload/{appointmentId}")
    @Operation(summary = "Upload documents for appointment")
    public ResponseEntity<List<DocumentResponse>> uploadDocuments(
            @PathVariable String appointmentId,
            @RequestParam("files") List<MultipartFile> files) {
        List<DocumentResponse> documents = documentService.uploadDocuments(appointmentId, files);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Get all documents for appointment")
    public ResponseEntity<List<DocumentResponse>> getAppointmentDocuments(@PathVariable String appointmentId) {
        List<DocumentResponse> documents = documentService.getAppointmentDocuments(appointmentId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document details")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String documentId) {
        DocumentResponse document = documentService.getDocument(documentId);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/download/{documentId}")
    @Operation(summary = "Download document")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentId) {
        DocumentResponse documentInfo = documentService.getDocument(documentId);
        byte[] content = documentService.downloadDocument(documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + documentInfo.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(documentInfo.getFileType()))
                .body(content);
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}