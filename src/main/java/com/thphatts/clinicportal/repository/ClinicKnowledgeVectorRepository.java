package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.ClinicKnowledgeVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicKnowledgeVectorRepository extends JpaRepository<ClinicKnowledgeVector, Long> {

    /**
     * PGVector Cosine Similarity Search: Tìm Top-K mẩu tri thức gần nhất với query vector.
     * Toán tử <=> (cosine distance), sắp xếp tăng dần (distance nhỏ = similarity cao).
     *
     * NOTE: CAST(:vec AS vector) bắt buộc vì JDBC không hỗ trợ native type 'vector'.
     *       spring.jpa.show-sql=true sẽ log query này để debug.
     */
    @Query(value = """
            SELECT content
            FROM clinic_knowledge_vectors
            WHERE embedding_vector IS NOT NULL
            ORDER BY embedding_vector <=> CAST(:vec AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<String> findTopKSimilarContent(@Param("vec") String vec, @Param("topK") int topK);

    /**
     * Tìm knowledge entry theo source entity để update khi entity thay đổi
     */
    Optional<ClinicKnowledgeVector> findBySourceEntityTypeAndSourceEntityId(
            String sourceEntityType, Long sourceEntityId);

    /**
     * Xóa toàn bộ entries theo category để re-index
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ClinicKnowledgeVector k WHERE k.sourceEntityType = :entityType")
    void deleteBySourceEntityType(@Param("entityType") String entityType);

    /**
     * Đếm số entries đã được vectorize (embedding_vector != null)
     */
    @Query(value = "SELECT COUNT(*) FROM clinic_knowledge_vectors WHERE embedding_vector IS NOT NULL",
           nativeQuery = true)
    long countIndexedEntries();
}
