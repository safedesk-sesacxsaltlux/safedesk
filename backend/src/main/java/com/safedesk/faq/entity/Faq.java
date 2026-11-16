package com.safedesk.faq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * faq 테이블 (schema.sql 참고)
 *   faq_id     bigint AUTO_INCREMENT PK
 *   category   varchar(50) NOT NULL   문의 유형
 *   question   varchar(255) NOT NULL
 *   answer     text NOT NULL
 *   created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
 *
 * 지금은 FAQ 매칭(CUS-02, INT-01)이 읽기만 한다. 등록·수정(API-ADM-09~11)은 이 엔티티에 메서드를 추가해서 쓴다.
 */
@Entity
@Table(name = "faq")
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long faqId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "question", nullable = false, length = 255)
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    // DB 기본값(CURRENT_TIMESTAMP)이 채우도록 JPA 는 INSERT·UPDATE 에서 이 컬럼을 뺀다.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Faq() {
    }

    public Long getFaqId() {
        return faqId;
    }

    public String getCategory() {
        return category;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
