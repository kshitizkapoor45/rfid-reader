package com.rfid;

import javax.swing.text.html.HTML;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagStorageService implements TagStorage {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public TagStorageService(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;

        try {
            Class.forName("org.postgresql.Driver");
            initTable();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize TagStorageService: " + e.getMessage(), e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    // ✅ Create table if not exists
    private void initTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS tag_details (
                    id SERIAL PRIMARY KEY,
                    tag_id VARCHAR(255) NOT NULL,
                    antenna INT NOT NULL,
                    first_seen TIMESTAMP,
                    last_seen TIMESTAMP,
                    CONSTRAINT unique_tag UNIQUE(tag_id, antenna)
                )
                """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<TagDetail> findByTagIdAndAntenna(String tagId, int antenna) {
        String sql = "SELECT tag_id, antenna, first_seen, last_seen FROM tag_details WHERE tag_id = ? AND antenna = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tagId);
            ps.setInt(2, antenna);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TagDetail(
                            rs.getString("tag_id"),
                            rs.getInt("antenna"),
                            rs.getTimestamp("first_seen").toInstant(),
                            rs.getTimestamp("last_seen").toInstant()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<TagDetail> findAll() {
        String sql = "SELECT tag_id, antenna, first_seen, last_seen FROM tag_details";
        List<TagDetail> tagDetails = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                TagDetail tagDetail = new TagDetail();
                tagDetail.setTagId(rs.getString("tag_id"));
                tagDetail.setAntenna(rs.getInt("antenna"));

                Timestamp firstSeenTs = rs.getTimestamp("first_seen");
                if (firstSeenTs != null) {
                    tagDetail.setFirstSeen(firstSeenTs.toInstant());
                }
                Timestamp lastSeenTs = rs.getTimestamp("last_seen");
                if (lastSeenTs != null) {
                    tagDetail.setLastSeen(lastSeenTs.toInstant());
                }
                tagDetails.add(tagDetail);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return tagDetails;
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM tag_details";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(TagDetail tag) {
        String sql = """
                INSERT INTO tag_details (tag_id, antenna, first_seen, last_seen)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (tag_id, antenna)
                DO UPDATE SET last_seen = EXCLUDED.last_seen
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag.getTagId());
            ps.setInt(2, tag.getAntenna());
            ps.setTimestamp(3, Timestamp.from(tag.getFirstSeen()));
            ps.setTimestamp(4, Timestamp.from(tag.getLastSeen()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}