package com.safedesk.faq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * faq_keyword 테이블 (schema.sql 참고)
 *   keyword_id bigint AUTO_INCREMENT PK
 *   faq_id     bigint NOT NULL FK → faq (ON DELETE CASCADE)
 *   keyword    varchar(50) NOT NULL   (faq_id, keyword) UNIQUE
 */
@Entity
@Table(name = "faq_keyword")
public class FaqKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Long keywordId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faq_id", nullable = false)
    private Faq faq;

    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;

    protected FaqKeyword() {
    }

    public Faq getFaq() {
        return faq;
    }

    public String getKeyword() {
        return keyword;
    }
}
