package com.gold.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 技术指标实体
 */
@Entity
@Table(name = "technical_indicator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicalIndicator {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "price_id")
    private Long priceId;            // 关联价格ID
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp; // 时间戳
    
    // 移动平均线
    @Column(name = "ma5", precision = 10, scale = 2)
    private BigDecimal ma5;
    
    @Column(name = "ma10", precision = 10, scale = 2)
    private BigDecimal ma10;
    
    @Column(name = "ma20", precision = 10, scale = 2)
    private BigDecimal ma20;
    
    // RSI
    @Column(name = "rsi14", precision = 5, scale = 2)
    private BigDecimal rsi14;
    
    // ATR
    @Column(name = "atr14", precision = 10, scale = 2)
    private BigDecimal atr14;
    
    // 涨跌幅
    @Column(name = "change_5m", precision = 5, scale = 2)
    private BigDecimal change5m;     // 5分钟涨跌幅
    
    @Column(name = "change_1h", precision = 5, scale = 2)
    private BigDecimal change1h;     // 1小时涨跌幅
    
    @Column(name = "change_1d", precision = 5, scale = 2)
    private BigDecimal change1d;     // 1天涨跌幅
    
    @Column(name = "change_week", precision = 5, scale = 2)
    private BigDecimal changeWeek;   // 本周涨跌幅
    
    @Column(name = "day_high", precision = 10, scale = 2)
    private BigDecimal dayHigh;      // 日内高点
    
    @Column(name = "day_low", precision = 10, scale = 2)
    private BigDecimal dayLow;       // 日内低点
    
    @Column(name = "week_high", precision = 10, scale = 2)
    private BigDecimal weekHigh;     // 本周高点
    
    @Column(name = "week_low", precision = 10, scale = 2)
    private BigDecimal weekLow;      // 本周低点
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
