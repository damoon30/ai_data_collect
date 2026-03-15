package com.gold.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易信号实体
 */
@Entity
@Table(name = "trading_signal")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingSignal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "signal_type", length = 50)
    private String signalType;       // 信号类型
    
    @Column(name = "signal_level", length = 20)
    private String signalLevel;      // 信号级别 INFO/WARNING/CRITICAL
    
    @Column(name = "current_price", precision = 10, scale = 2)
    private BigDecimal currentPrice; // 当前价格
    
    @Column(name = "trigger_value", precision = 10, scale = 2)
    private BigDecimal triggerValue; // 触发值
    
    @Column(name = "change_percent", precision = 5, scale = 2)
    private BigDecimal changePercent;// 变动幅度
    
    @Column(name = "direction", length = 10)
    private String direction;        // 方向 UP/DOWN
    
    @Column(name = "description", length = 500)
    private String description;      // 信号描述
    
    @Column(name = "technical_analysis", length = 1000)
    private String technicalAnalysis;// 技术分析
    
    @Column(name = "risk_warning", length = 500)
    private String riskWarning;      // 风险提示
    
    @Column(name = "pushed")
    private Boolean pushed = false;  // 是否已推送
    
    @Column(name = "push_time")
    private LocalDateTime pushTime;  // 推送时间
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
