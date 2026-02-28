package com.certforge.repository;

import com.certforge.model.CertHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertHistoryRepository extends MongoRepository<CertHistory, String> {

    List<CertHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    void deleteByUserId(String userId);

    // Search for a session that has a certificate with the given certificate number
    // This uses MongoDB dot-notation query on the embedded array
    @Query("{ 'certificates.certificateNumber': ?0 }")
    Optional<CertHistory> findByCertificateNumber(String certificateNumber);
}
