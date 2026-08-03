package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Attachment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AttachmentRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(Attachment attachment) {
        String sql = """
                INSERT INTO attachments (
                    module,
                    entity_id,
                    original_name,
                    stored_name,
                    file_path,
                    content_type,
                    file_size
                )
                VALUES (?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, attachment.getModule());
            statement.setLong(2, attachment.getEntityId());
            statement.setString(3, attachment.getOriginalName());
            statement.setString(4, attachment.getStoredName());
            statement.setString(5, attachment.getFilePath());
            statement.setString(6, attachment.getContentType());
            statement.setLong(7, attachment.getFileSize());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save attachment.", exception);
        }
    }

    public List<Attachment> findByEntity(String module, Long entityId) {
        String sql = """
                SELECT id,
                       module,
                       entity_id,
                       original_name,
                       stored_name,
                       file_path,
                       content_type,
                       file_size,
                       created_at
                FROM attachments
                WHERE module = ?
                  AND entity_id = ?
                ORDER BY created_at DESC, id DESC;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, module);
            statement.setLong(2, entityId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Attachment> attachments = new ArrayList<>();

                while (resultSet.next()) {
                    attachments.add(mapAttachment(resultSet));
                }

                return attachments;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list attachments.", exception);
        }
    }

    public void deleteByEntity(String module, Long entityId) {
        String sql = """
                DELETE FROM attachments
                WHERE module = ?
                  AND entity_id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, module);
            statement.setLong(2, entityId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete attachments.", exception);
        }
    }

    public int countByEntity(String module, Long entityId) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM attachments
                WHERE module = ?
                  AND entity_id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, module);
            statement.setLong(2, entityId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0;
                }

                return resultSet.getInt("total");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count attachments.", exception);
        }
    }

    private Attachment mapAttachment(ResultSet resultSet) throws SQLException {
        return new Attachment(
                resultSet.getLong("id"),
                resultSet.getString("module"),
                resultSet.getLong("entity_id"),
                resultSet.getString("original_name"),
                resultSet.getString("stored_name"),
                resultSet.getString("file_path"),
                resultSet.getString("content_type"),
                resultSet.getLong("file_size"),
                LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME)
        );
    }
}
