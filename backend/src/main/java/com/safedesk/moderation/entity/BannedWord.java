package com.safedesk.moderation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * banned_word 테이블 (schema.sql 참고)
 *   word_id    bigint AUTO_INCREMENT PK
 *   word       varchar(50) NOT NULL UNIQUE
 *   created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 *
 * ddl-auto: validate 이므로 컬럼 이름·타입이 schema.sql 과 맞아야 앱이 뜬다.
 */
@Entity
@Table(name = "banned_word")
public class BannedWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Long wordId;

    @Column(name = "word", nullable = false, length = 50, unique = true)
    private String word;

    // DB 기본값(CURRENT_TIMESTAMP)이 채우도록 JPA 는 INSERT·UPDATE 에서 이 컬럼을 뺀다.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BannedWord() {
    }

    public String getWord() {
        return word;
    }
}
