package com.gold.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 黄金价格监控配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gold.monitor")
public class GoldMonitorConfig {
    
    private PriceSourceConfig priceSource;
    private CollectorConfig collector;
    private IndicatorConfig indicator;
    private SignalConfig signal;
    private PusherConfig pusher;
    private AdvisorConfig advisor;
    
    @Data
    public static class PriceSourceConfig {
        private String primary;
        private String backup;
        private String apiUrl;
    }
    
    @Data
    public static class CollectorConfig {
        private int intervalMinutes;
        private int retryTimes;
        private long retryDelayMs;
        private long timeoutMs;
        private int historyDays;
    }
    
    @Data
    public static class IndicatorConfig {
        private List<Integer> maPeriods;
        private int rsiPeriod;
        private int atrPeriod;
    }
    
    @Data
    public static class SignalConfig {
        private ChangeThresholdConfig changeThreshold;
        private BreakoutConfig breakout;
        private int cooldownMinutes;
    }
    
    @Data
    public static class ChangeThresholdConfig {
        private double minute5;
        private double hour1;
        private double day1;
    }
    
    @Data
    public static class BreakoutConfig {
        private boolean enableDayHighLow;
        private boolean enableMaCross;
    }
    
    @Data
    public static class PusherConfig {
        private FeishuConfig feishu;
    }
    
    @Data
    public static class FeishuConfig {
        private boolean enabled;
        private String webhookUrl;
        private String chatId;
    }
    
    @Data
    public static class AdvisorConfig {
        private PositionConfig position;
        private StopLossConfig stopLoss;
    }
    
    @Data
    public static class PositionConfig {
        private List<Integer> conservative;
        private List<Integer> balanced;
        private List<Integer> aggressive;
    }
    
    @Data
    public static class StopLossConfig {
        private double defaultPercent;
        private double tightPercent;
        private double loosePercent;
    }
}
