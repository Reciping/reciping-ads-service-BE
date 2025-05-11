package com.three.recipingadsservicebe.ad.repository;

import com.three.recipingadsservicebe.ad.entity.Ad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdRepository extends JpaRepository<Ad, Long> {

}
