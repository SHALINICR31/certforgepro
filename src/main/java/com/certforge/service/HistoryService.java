package com.certforge.service;

import com.certforge.model.CertHistory;
import com.certforge.repository.CertHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);

    @Autowired private CertHistoryRepository historyRepository;
    @Autowired private MongoTemplate mongoTemplate;

    public CertHistory save(CertHistory history) {
        return historyRepository.save(history);
    }

    public List<CertHistory> getHistoryForUser(String userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void deleteAllForUser(String userId) {
        historyRepository.deleteByUserId(userId);
    }

    public void deleteSession(String sessionId) {
        historyRepository.deleteById(sessionId);
    }

    /** Returns true if the given session belongs to the given user — prevents IDOR */
    public boolean sessionBelongsToUser(String sessionId, String userId) {
        return historyRepository.findById(sessionId)
            .map(s -> userId.equals(s.getUserId()))
            .orElse(false);
    }

    public long countAll() {
        return historyRepository.count();
    }

    /**
     * Find a certificate record by its number.
     * Uses MongoTemplate for a reliable embedded-array element match query.
     */
    public Optional<CertHistory.CertRecord> findByCertNumber(String certNumber) {
        log.info("Searching DB for certificate: '{}'", certNumber);

        try {
            // Use MongoTemplate — more reliable than @Query for embedded arrays
            Query q = new Query(
                Criteria.where("certificates.certificateNumber").is(certNumber)
            );
            CertHistory session = mongoTemplate.findOne(q, CertHistory.class);

            if (session == null) {
                long total = historyRepository.count();
                log.warn("Certificate '{}' not found. Total sessions in DB: {}", certNumber, total);
                return Optional.empty();
            }

            log.info("Found session '{}' containing certificate '{}'", session.getId(), certNumber);

            return session.getCertificates().stream()
                .filter(r -> certNumber.equals(r.getCertificateNumber()))
                .findFirst();

        } catch (Exception e) {
            log.error("Error searching for certificate '{}': {}", certNumber, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
