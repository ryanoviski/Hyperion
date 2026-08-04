package com.hyperion.service;

import com.hyperion.model.Attachment;
import com.hyperion.repository.AttachmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttachmentService {

    public static final String FINANCE_MODULE = "FINANCE";

    private static final Path ATTACHMENTS_DIRECTORY = Path.of("data", "attachments");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AttachmentRepository attachmentRepository = new AttachmentRepository();

    public void attachFile(String module, Long entityId, Path sourceFile) {
        String normalizedModule = normalize(module);

        if (normalizedModule.isBlank()) {
            throw new IllegalArgumentException("Informe o módulo do anexo.");
        }

        if (entityId == null) {
            throw new IllegalArgumentException("Informe o registro vinculado ao anexo.");
        }

        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("Selecione um arquivo válido.");
        }

        try {
            Path moduleDirectory = ATTACHMENTS_DIRECTORY
                    .resolve(normalizedModule.toLowerCase())
                    .resolve(String.valueOf(entityId));
            Files.createDirectories(moduleDirectory);

            String originalName = sourceFile.getFileName().toString();
            String storedName = createStoredFileName(originalName);
            Path targetFile = moduleDirectory.resolve(storedName);

            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            attachmentRepository.save(new Attachment(
                    normalizedModule,
                    entityId,
                    originalName,
                    storedName,
                    targetFile.toString(),
                    Files.probeContentType(sourceFile),
                    Files.size(targetFile)
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível salvar o anexo.", exception);
        }
    }

    public List<Attachment> listAttachments(String module, Long entityId) {
        if (entityId == null) {
            return List.of();
        }

        return attachmentRepository.findByEntity(normalize(module), entityId);
    }

    public int countAttachments(String module, Long entityId) {
        if (entityId == null) {
            return 0;
        }

        return attachmentRepository.countByEntity(normalize(module), entityId);
    }

    public Path resolveAttachmentPath(Attachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("Selecione um anexo para visualizar.");
        }

        Path filePath = Path.of(attachment.getFilePath());

        if (!Files.exists(filePath)) {
            throw new IllegalStateException("O arquivo do anexo não foi encontrado.");
        }

        return filePath;
    }

    public void deleteByEntity(String module, Long entityId) {
        if (entityId == null) {
            return;
        }

        String normalizedModule = normalize(module);
        List<Attachment> attachments = attachmentRepository.findByEntity(normalizedModule, entityId);

        for (Attachment attachment : attachments) {
            try {
                Files.deleteIfExists(Path.of(attachment.getFilePath()));
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível remover o anexo.", exception);
            }
        }

        attachmentRepository.deleteByEntity(normalizedModule, entityId);
    }

    private String createStoredFileName(String originalName) {
        String extension = "";
        int extensionStart = originalName.lastIndexOf('.');

        if (extensionStart >= 0 && extensionStart < originalName.length() - 1) {
            extension = originalName.substring(extensionStart);
        }

        return FILE_TIMESTAMP.format(LocalDateTime.now()) + "_" + sanitizeFileName(originalName) + extension;
    }

    private String sanitizeFileName(String fileName) {
        String nameWithoutExtension = fileName;
        int extensionStart = fileName.lastIndexOf('.');

        if (extensionStart > 0) {
            nameWithoutExtension = fileName.substring(0, extensionStart);
        }

        String sanitized = nameWithoutExtension
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");

        return sanitized.isBlank() ? "arquivo" : sanitized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
