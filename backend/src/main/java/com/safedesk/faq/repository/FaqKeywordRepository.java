package com.safedesk.faq.repository;

import com.safedesk.faq.dto.FaqKeywordRow;
import com.safedesk.faq.entity.FaqKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FaqKeywordRepository extends JpaRepository<FaqKeyword, Long> {

    /**
     * 매칭에 필요한 값만 한 번의 조인 쿼리로 가져온다. (FAQ 30건·키워드 약 200개 규모)
     */
    @Query("""
            select new com.safedesk.faq.dto.FaqKeywordRow(f.faqId, f.category, f.question, f.answer, k.keyword)
            from FaqKeyword k
            join k.faq f
            """)
    List<FaqKeywordRow> findAllRows();
}
