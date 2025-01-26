package com.fileprocessingapplication.dao;

import com.fileprocessingapplication.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByFileName(String fileName);
}

