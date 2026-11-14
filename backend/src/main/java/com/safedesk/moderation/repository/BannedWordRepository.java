package com.safedesk.moderation.repository;

import com.safedesk.moderation.entity.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {
    // 전체 조회(findAll)만 쓴다면 추가 메서드는 필요 없다.
}
