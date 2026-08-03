package com.hyperion.model;

import java.time.LocalDateTime;

public class Attachment {

    private final Long id;
    private final String module;
    private final Long entityId;
    private final String originalName;
    private final String storedName;
    private final String filePath;
    private final String contentType;
    private final long fileSize;
    private final LocalDateTime createdAt;

    public Attachment(
            String module,
            Long entityId,
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize
    ) {
        this(null, module, entityId, originalName, storedName, filePath, contentType, fileSize, null);
    }

    public Attachment(
            Long id,
            String module,
            Long entityId,
            String originalName,
            String storedName,
            String filePath,
            String contentType,
            long fileSize,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.module = module;
        this.entityId = entityId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getModule() {
        return module;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
