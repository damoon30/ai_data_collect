package com.gold.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 黄金价格实体
 */
@Entity
@Table(name = "gold_price", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_symbol", columnList = "symbol")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldPrice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;           // 品种代码 XAUUSD/GC=F
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp; // 时间戳
    
    @Column(name = "open", precision = 10, scale = 2)
    private BigDecimal open;         // 开盘价
    
    @Column(name = "high", precision = 10, scale = 2)
    private BigDecimal high;         // 最高价
    
    @Column(name = "low", precision = 10, scale = 2)
    private BigDecimal low;          // 最低价
    
    @Column(name = "close", precision = 10, scale = 2)
    private BigDecimal close;        // 收盘价/当前价
    
    @Column(name = "volume")
    private Long volume;             // 成交量
    
    @Column(name = "created_at")
    private LocalDateTime createdAt; // 记录创建时间
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
