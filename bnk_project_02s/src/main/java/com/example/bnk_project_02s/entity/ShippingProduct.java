package com.example.bnk_project_02s.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_shipping_product")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShippingProduct {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spno;

    private String spname;
    private String spdescription;

    private Double spprice;
    private String spcurrency;

    private Double sprating;
    private int spreviews;

    private String spurl;

    @Column(name = "spat", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime spat;
}
