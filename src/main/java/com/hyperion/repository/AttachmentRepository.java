package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Attachment;
import com.hyperion.exception.PersistenceException;
import com.hyperion.util.SqliteTimestamp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AttachmentRepository {

    private static final int MAX_ENTITY_IDS_PER_COUNT_QUERY = 900;

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
            throw new PersistenceException("Não foi possível salvar o anexo.", exception);
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
            throw new PersistenceException("Não foi possível listar os anexos.", exception);
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
            throw new PersistenceException("Não foi possível remover os anexos.", exception);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM attachments WHERE id = ?;";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new PersistenceException("Não foi possível localizar o anexo para remoção.");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível remover o anexo.", exception);
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
            throw new PersistenceException("Não foi possível contar os anexos.", exception);
        }
    }

    public Map<Long, Integer> countByEntities(String module, List<Long> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (int startIndex = 0; startIndex < entityIds.size(); startIndex += MAX_ENTITY_IDS_PER_COUNT_QUERY) {
            int endIndex = Math.min(startIndex + MAX_ENTITY_IDS_PER_COUNT_QUERY, entityIds.size());
            counts.putAll(countByEntityBatch(module, entityIds.subList(startIndex, endIndex)));
        }
        return counts;
    }

    private Map<Long, Integer> countByEntityBatch(String module, List<Long> entityIds) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(entityIds.size(), "?"));
        String sql = """
                SELECT entity_id, COUNT(*) AS total
                FROM attachments
                WHERE module = ?
                  AND entity_id IN (%s)
                GROUP BY entity_id;
                """.formatted(placeholders);

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, module);
            for (int index = 0; index < entityIds.size(); index++) {
                statement.setLong(index + 2, entityIds.get(index));
            }

            Map<Long, Integer> counts = new LinkedHashMap<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    counts.put(resultSet.getLong("entity_id"), resultSet.getInt("total"));
                }
            }
            return counts;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível contar os anexos.", exception);
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
                SqliteTimestamp.toLocalDateTime(resultSet.getString("created_at"), "criação do anexo")
        );
    }
}
