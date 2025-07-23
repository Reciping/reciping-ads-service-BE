package com.three.recipingadsservicebe.abtest.entity;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ab_test_events")
public class AbTestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "scenario_code", nullable = false, length = 50)
    private String scenarioCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ab_test_group", nullable = false, length = 15)
    private AbTestGroup abTestGroup;

    @Column(name = "ad_id")
    private Long adId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType; // IMPRESSION, CLICK

    @Column(name = "position", nullable = false, length = 50)
    private String position;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 편의 메서드들
    public static AbTestEvent createImpression(Long userId, String scenarioCode,
                                               AbTestGroup group, Long adId, String position) {
        return AbTestEvent.builder()
                .userId(userId)
                .scenarioCode(scenarioCode)
                .abTestGroup(group)
                .adId(adId)
                .eventType("IMPRESSION")
                .position(position)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AbTestEvent createClick(Long userId, String scenarioCode,
                                          AbTestGroup group, Long adId, String position) {
        return AbTestEvent.builder()
                .userId(userId)
                .scenarioCode(scenarioCode)
                .abTestGroup(group)
                .adId(adId)
                .eventType("CLICK")
                .position(position)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
