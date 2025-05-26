package com.three.recipingadsservicebe.advertiser.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE advertisers SET deleted_at = now() WHERE id = ?")
@SQLRestriction(value = "deleted_at IS NULL")
@Entity
@Table(name = "advertisers")
public class Advertiser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;


}

