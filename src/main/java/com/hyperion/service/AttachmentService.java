package com.hyperion.service;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.exception.HyperionException;
import com.hyperion.exception.PartialOperationException;
import com.hyperion.model.Attachment;
import com.hyperion.repository.AttachmentRepository;
import com.hyperion.exception.AttachmentStorageException;
import com.hyperion.exception.InvalidAttachmentModuleException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AttachmentService {

    public static final String FINANCE_MODULE = "FINANCE";

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");

    private final AttachmentRepository attachmentRepository = new AttachmentRepository();

    public void attachFile(String module, Long entityId, Path sourceFile) {
        String normalizedModule = normalize(module);
        Path targetFile = null;

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
            Path attachmentsDirectory = getAttachmentsDirectory();
            Path moduleDirectory = attachmentsDirectory
                    .resolve(normalizedModule.toLowerCase())
                    .resolve(String.valueOf(entityId));
            Files.createDirectories(moduleDirectory);

            String originalName = sourceFile.getFileName().toString();
            String storedName = createStoredFileName(validatedAttachment.extension());
            targetFile = moduleDirectory.resolve(storedName);

            Files.copy(sourceFile, targetFile);

            attachmentRepository.save(new Attachment(
                    normalizedModule,
                    entityId,
                    originalName,
                    storedName,
                    attachmentsDirectory.relativize(targetFile).toString(),
                    validatedAttachment.contentType(),
                    Files.size(targetFile)
            ));
        } catch (IOException exception) {
            cleanupUnregisteredFile(targetFile, exception);
            throw new AttachmentStorageException("Não foi possível salvar o anexo.", exception);
        } catch (HyperionException exception) {
            cleanupUnregisteredFile(targetFile, exception);
            throw exception;
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

    public Map<Long, Integer> countAttachments(String module, List<Long> entityIds) {
        return attachmentRepository.countByEntities(normalize(module), entityIds);
    }

    public Path resolveAttachmentPath(Attachment attachment) {
        if (attachment == null) {
            throw new com.hyperion.exception.ValidationException("Selecione um anexo para visualizar.");
        }

        return resolveStoredPath(attachment, true);
    }

    public void deleteByEntity(String module, Long entityId) {
        if (entityId == null) {
            return;
        }

        String normalizedModule = normalize(module);
        List<Attachment> attachments = attachmentRepository.findByEntity(normalizedModule, entityId);
        List<Path> attachmentFiles = new ArrayList<>();

        for (Attachment attachment : attachments) {
            attachmentFiles.add(resolveStoredPath(attachment, false));
        }

        attachmentRepository.deleteByEntity(normalizedModule, entityId);

        for (Path attachmentFile : attachmentFiles) {
            try {
                Files.deleteIfExists(attachmentFile);
            } catch (IOException exception) {
                throw new PartialOperationException(
                        "Os registros dos anexos foram removidos, mas alguns arquivos não puderam ser excluídos.",
                        exception
                );
            }
        }
    }

    public void deleteAttachment(Attachment attachment) {
        if (attachment == null || attachment.getId() == null) {
            throw new com.hyperion.exception.ValidationException("Selecione um anexo para remover.");
        }

        Path filePath = resolveStoredPath(attachment, false);
        attachmentRepository.delete(attachment.getId());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new PartialOperationException(
                    "O registro do anexo foi removido, mas o arquivo físico não pôde ser excluído.",
                    exception
            );
        }
    }

    private String createStoredFileName(String extension) {
        return UUID.randomUUID() + "." + extension;
    }

    public Path getAttachmentsDirectory() {
        return DatabaseConfig.getDataDirectory().resolve("attachments").toAbsolutePath().normalize();
    }

    private Path resolveStoredPath(Attachment attachment, boolean requireExistingFile) {
        if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
            throw new AttachmentStorageException("O anexo não possui um caminho de armazenamento válido.");
        }

        Path storedPath;
        try {
            storedPath = Path.of(attachment.getFilePath());
        } catch (InvalidPathException exception) {
            throw new AttachmentStorageException("O caminho do anexo é inválido.", exception);
        }

        Path attachmentsDirectory = getAttachmentsDirectory();
        Path filePath = (storedPath.isAbsolute() ? storedPath : attachmentsDirectory.resolve(storedPath))
                .toAbsolutePath()
                .normalize();

        if (!filePath.startsWith(attachmentsDirectory)) {
            throw new AttachmentStorageException("O anexo está fora da área de armazenamento segura do Hyperion.");
        }

        if (requireExistingFile && !Files.isRegularFile(filePath)) {
            throw new AttachmentStorageException("O arquivo do anexo não foi encontrado.");
        }

        if (Files.exists(filePath)) {
            try {
                Path realRoot = Files.exists(attachmentsDirectory)
                        ? attachmentsDirectory.toRealPath()
                        : attachmentsDirectory;
                if (!filePath.toRealPath().startsWith(realRoot)) {
                    throw new AttachmentStorageException("O anexo está fora da área de armazenamento segura do Hyperion.");
                }
            } catch (IOException exception) {
                throw new AttachmentStorageException("Não foi possível validar o caminho do anexo.", exception);
            }
        }

        return filePath;
    }

    private void cleanupUnregisteredFile(Path targetFile, Exception originalException) {
        if (targetFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
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
