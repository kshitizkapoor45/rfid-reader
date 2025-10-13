package com.rfid;

import java.util.List;
import java.util.Optional;

public interface TagStorage {
    Optional<TagDetail> findByTagIdAndReader(String tagId, String reader);
    void save(TagDetail tag);
    List<TagDetail> findAll();
    void deleteAll();
    List<TagDetail> findReaderIp();
}