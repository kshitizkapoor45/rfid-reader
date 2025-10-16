package com.rfid;

import javax.swing.text.html.HTML;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                    reader_ip VARCHAR(255) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'NOT_SYNCED'
                                CHECK (status IN ('SYNCED', 'NOT_SYNCED')),
                    CONSTRAINT unique_tag UNIQUE(tag_id, reader_ip)
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
    public Optional<TagDetail> findByTagIdAndReader(String tagId, String reader) {
        String sql = "SELECT tag_id, antenna, first_seen, last_seen, reader_ip FROM tag_details WHERE tag_id = ? AND reader_ip = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tagId);
            ps.setString(2,reader);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TagDetail(
                            rs.getString("tag_id"),
                            rs.getInt("antenna"),
                            rs.getTimestamp("first_seen").toInstant(),
                            rs.getTimestamp("last_seen").toInstant(),
                            rs.getString("reader_ip")
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
        String sql = "SELECT tag_id, antenna, first_seen, last_seen, reader_ip FROM tag_details";
        List<TagDetail> tagDetails = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                TagDetail tagDetail = new TagDetail();
                tagDetail.setTagId(rs.getString("tag_id"));
                tagDetail.setAntenna(rs.getInt("antenna"));
                tagDetail.setReader(rs.getString("reader_ip"));

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
    public void deleteByReaderIps(List<String> readerIps) {
        if (readerIps == null || readerIps.isEmpty()) {
            return;
        }
        String placeholders = readerIps.stream()
                .map(ip -> "?")
                .collect(Collectors.joining(", "));

        String sql = "DELETE FROM tag_details WHERE reader_ip IN (" + placeholders + ")";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < readerIps.size(); i++) {
                ps.setString(i + 1, readerIps.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TagDetail> fetchUnsyncedIpTags() {
        String sql = "SELECT DISTINCT reader_ip FROM tag_details";
        List<TagDetail> tagDetails = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                TagDetail tagDetail = new TagDetail();
                tagDetail.setReader(rs.getString("reader_ip"));
                tagDetails.add(tagDetail);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return tagDetails;
    }

    @Override
    public List<TagDetail> findTagDetailsByIp(String ip) {
        String sql = "SELECT tag_id, antenna, first_seen, last_seen, reader_ip, status FROM tag_details WHERE reader_ip = ?";
        List<TagDetail> tagDetails = new ArrayList<>();

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, ip); // set parameter
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TagDetail tagDetail = new TagDetail();
                    tagDetail.setTagId(rs.getString("tag_id"));
                    tagDetail.setAntenna(rs.getInt("antenna"));
                    tagDetail.setReader(rs.getString("reader_ip"));

                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        try {
                            tagDetail.setStatus(TagStatus.valueOf(statusStr));
                        } catch (IllegalArgumentException ex) {
                            System.err.println("Unknown status: " + statusStr);
                        }
                    }
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
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
        return tagDetails;
    }

    @Override
    public void save(TagDetail tag) {
        String sql = """
                INSERT INTO tag_details (tag_id, antenna, first_seen, last_seen, reader_ip)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (tag_id,reader_ip)
                DO UPDATE SET last_seen = EXCLUDED.last_seen
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag.getTagId());
            ps.setInt(2, tag.getAntenna());
            ps.setTimestamp(3, Timestamp.from(tag.getFirstSeen()));
            ps.setTimestamp(4, Timestamp.from(tag.getLastSeen()));
            ps.setString(5,tag.getReader());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}