package com.rfid;

import java.util.List;
import java.util.Optional;

public interface TagStorage {
    Optional<TagDetail> findByTagIdAndAntenna(String tagId, int antenna);
    void save(TagDetail tag);
    List<TagDetail> findAll();
    void deleteAll();
}
