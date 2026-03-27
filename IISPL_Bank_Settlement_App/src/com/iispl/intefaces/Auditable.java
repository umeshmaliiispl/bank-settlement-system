package com.iispl.intefaces;


import java.time.LocalDateTime;

/**
 * Auditable interface — implemented by BaseEntity.
 * Ensures every domain object tracks who created it and when it was last changed.
 */
public interface Auditable {
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    String getCreatedBy();
    void markUpdated();
}