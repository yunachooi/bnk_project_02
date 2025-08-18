package com.example.bnk_project_02s.entity;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_shopping_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slno")
    private Long slno;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", referencedColumnName = "uid")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spno", referencedColumnName = "spno")
    private ShoppingProducts shoppingProducts;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cardno", referencedColumnName = "cardno")
    private Card card;
    
    @Column(name = "slamount")
    private String slamount;
    
    @Column(name = "slcurrency")
    private String slcurrency;
    
    @Column(name = "slstatus")
    @Builder.Default
    private String slstatus = "P";
    
    @Column(name = "slreason")
    private String slreason;
    
    @CreationTimestamp
    @Column(name = "slreqat")
    private LocalDateTime slreqat;
    
    @Column(name = "slcomat")
    private LocalDateTime slcomat;
}
