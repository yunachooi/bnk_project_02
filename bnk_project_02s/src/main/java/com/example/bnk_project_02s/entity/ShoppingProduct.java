package com.example.bnk_project_02s.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_shopping_product")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingProduct {
    
    @Id
    private String spno;
    
    private String spname;
    
    @Column(length = 1000)
    private String spnameKo;
    
    @Column(length = 2000)
    private String spdescription;
    
    @Column(length = 3000)
    private String spdescriptionKo;
    
    private Double spprice;
    private String spcurrency;
    private Double sprating;
    private Integer spreviews;
    
    @Column(length = 2000)
    private String spimgurl;
    
    @Column(length = 2000)
    private String spurl;
    
    @Column(name = "spat", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime spat;
    
    @PrePersist
    protected void onCreate() {
        if (spat == null) {
            spat = LocalDateTime.now();
        }
    }
}