package us.hogu.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import us.hogu.model.AuditLog;

public interface AuditLogJpa extends JpaRepository<AuditLog, Long> {
        
}
