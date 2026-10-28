package com.rounak.productsecureapi.repository;

import com.rounak.productsecureapi.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface AuditRepository
        extends JpaRepository<AuditLog, Long> {
}
