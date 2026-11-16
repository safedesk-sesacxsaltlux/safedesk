package com.safedesk.moderation.repository;

import com.safedesk.moderation.entity.AbuseLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * abuse_log 저장소.
 *
 */
public interface AbuseLogRepository extends JpaRepository<AbuseLog, Long> {
}
