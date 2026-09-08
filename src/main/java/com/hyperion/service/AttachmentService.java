package com.hyperion.service;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Attachment;
import com.hyperion.repository.AttachmentRepository;
import com.hyperion.exception.AttachmentStorageException;
import com.hyperion.exception.InvalidAttachmentModuleException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AttachmentService {

    public static final String FINANCE_MODULE = "FINANCE";

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");

    private final AttachmentRepository attachmentRepository = new AttachmentRepository();

    public void attachFile(String module, Long entityId, Path sourceFile) {
        String normalizedModule = normalize(module);

        if (!FINANCE_MODULE.equals(normalizedModule)) {
            throw new InvalidAttachmentModuleException();
        }

        if (entityId == null) {
            throw new com.hyperion.exception.ValidationException("Informe o registro vinculado ao anexo.");
        }

        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new com.hyperion.exception.ValidationException("Selecione um arquivo válido.");
        }

        try {
            ValidatedAttachment validatedAttachment = validateSourceFile(sourceFile);
            Path moduleDirectory = getAttachmentsDirectory()
                    .resolve(normalizedModule.toLowerCase())
                    .resolve(String.valueOf(entityId));
            Files.createDirectories(moduleDirectory);

            String originalName = sourceFile.getFileName().toString();
            String storedName = createStoredFileName(validatedAttachment.extension());
            Path targetFile = moduleDirectory.resolve(storedName);

            Files.copy(sourceFile, targetFile);

            attachmentRepository.save(new Attachment(
                    normalizedModule,
                    entityId,
                    originalName,
                    storedName,
                    targetFile.toString(),
                    validatedAttachment.contentType(),
                    Files.size(targetFile)
            ));
        } catch (IOException exception) {
            throw new AttachmentStorageException("Não foi possível salvar o anexo.", exception);
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
            throw new com.hyperion.exception.ValidationException("Selecione um anexo para visualizar.");
        }

        Path filePath;
        try {
            filePath = Path.of(attachment.getFilePath()).normalize();
        } catch (InvalidPathException exception) {
            throw new AttachmentStorageException("O caminho do anexo é inválido.", exception);
        }

        if (!Files.exists(filePath)) {
            throw new AttachmentStorageException("O arquivo do anexo não foi encontrado.");
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
                throw new AttachmentStorageException("Não foi possível remover o anexo.", exception);
            }
        }

        attachmentRepository.deleteByEntity(normalizedModule, entityId);
    }

    public void deleteAttachment(Attachment attachment) {
        if (attachment == null || attachment.getId() == null) {
            throw new com.hyperion.exception.ValidationException("Selecione um anexo para remover.");
        }

        try {
            Files.deleteIfExists(Path.of(attachment.getFilePath()));
        } catch (IOException | InvalidPathException exception) {
            throw new AttachmentStorageException("Não foi possível remover o arquivo do anexo.", exception);
        }

        attachmentRepository.delete(attachment.getId());
    }

    private String createStoredFileName(String extension) {
        return UUID.randomUUID() + "." + extension;
    }

    private Path getAttachmentsDirectory() {
        return DatabaseConfig.getDataDirectory().resolve("attachments");
    }

    private ValidatedAttachment validateSourceFile(Path sourceFile) throws IOException {
        long fileSize = Files.size(sourceFile);
        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE_BYTES) {
            throw new com.hyperion.exception.ValidationException("O anexo deve ter entre 1 byte e 10 MB.");
        }

        String extension = fileExtension(sourceFile);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new com.hyperion.exception.ValidationException("Envie apenas arquivos PDF, PNG ou JPG.");
        }

        byte[] header;
        try (var input = Files.newInputStream(sourceFile)) {
            header = input.readNBytes(8);
        }
        String contentType = switch (extension) {
            case "pdf" -> isPdf(header) ? "application/pdf" : null;
            case "png" -> isPng(header) ? "image/png" : null;
            case "jpg", "jpeg" -> isJpeg(header) ? "image/jpeg" : null;
            default -> null;
        };

        if (contentType == null) {
            throw new com.hyperion.exception.ValidationException("O conteúdo do anexo não corresponde ao formato informado.");
        }
        return new ValidatedAttachment(extension, contentType);
    }

    private String fileExtension(Path file) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1
                ? ""
                : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isPdf(byte[] header) {
        return header.length >= 5
                && header[0] == '%'
                && header[1] == 'P'
                && header[2] == 'D'
                && header[3] == 'F'
                && header[4] == '-';
    }

    private boolean isPng(byte[] header) {
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (header.length < pngSignature.length) {
            return false;
        }
        for (int index = 0; index < pngSignature.length; index++) {
            if (header[index] != pngSignature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record ValidatedAttachment(String extension, String contentType) {
    }
}
