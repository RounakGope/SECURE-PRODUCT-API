package com.rounak.productsecureapi.serviceImpl;

import com.rounak.productsecureapi.entity.AuditLog;
import com.rounak.productsecureapi.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditRepository auditLogRepository;

    public AuditLogService(AuditRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(
            String username,
            String action,
            String entityType,
            String entityId,
            String status,
            String details) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setStatus(status);
        auditLog.setDetails(details);

        auditLogRepository.saveAndFlush(auditLog);
    }
}