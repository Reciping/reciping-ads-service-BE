package com.three.recipingadsservicebe.common.timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Timestamp {

    @CreatedDate
    @Column(updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        this.createdAt = now.toOffsetDateTime();
    }

}
