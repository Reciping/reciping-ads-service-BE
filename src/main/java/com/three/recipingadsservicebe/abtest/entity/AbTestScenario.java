package com.three.recipingadsservicebe.abtest.entity;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.segment.enums.MessageType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ab_test_scenarios")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbTestScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentType segment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_group", nullable = false, length = 10) // group - postgresql 예약어
    private AbTestGroup group; // A/B/CONTROL

    @Column(nullable = false, unique = true, length = 100)
    private String scenarioCode;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}

