package com.firmament.immigration.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private String id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String url;
}
