package com.gold.monitor.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gold.monitor.config.GoldMonitorConfig;
import com.gold.monitor.entity.GoldPrice;
import com.gold.monitor.repository.GoldPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 黄金价格采集器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCollector {
    
    private final GoldMonitorConfig config;
    private final GoldPriceRepository priceRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 每5分钟采集一次价格
     */
    @Scheduled(fixedRateString = "${gold.monitor.collector.interval-minutes:5}00000")
    public void collect() {
        try {
            log.info("开始采集黄金价格...");
            
            Optional<GoldPrice> priceOpt = fetchPriceWithRetry(config.getPriceSource().getPrimary());
            
            if (priceOpt.isEmpty()) {
                log.warn("主数据源获取失败，尝试备选数据源...");
                priceOpt = fetchPriceWithRetry(config.getPriceSource().getBackup());
            }
            
            priceOpt.ifPresent(this::savePrice);
            
        } catch (Exception e) {
            log.error("价格采集异常", e);
        }
    }
    
    /**
     * 带重试的价格获取
     */
    private Optional<GoldPrice> fetchPriceWithRetry(String symbol) {
        int retryTimes = config.getCollector().getRetryTimes();
        
        for (int i = 0; i < retryTimes; i++) {
            try {
                Optional<GoldPrice> price = fetchPrice(symbol);
                if (price.isPresent()) {
                    return price;
                }
            } catch (Exception e) {
                log.warn("第{}次尝试获取价格失败: {}", i + 1, e.getMessage());
                if (i < retryTimes - 1) {
                    Thread.sleep(config.getCollector().getRetryDelayMs());
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 从Yahoo Finance获取价格
     */
    private Optional<GoldPrice> fetchPrice(String symbol) throws Exception {
        String url = config.getPriceSource().getApiUrl() + symbol + "?interval=1m&range=1d";
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 200) {
                    log.warn("请求失败，状态码: {}", response.getCode());
                    return Optional.empty();
                }
                
                String json = EntityUtils.toString(response.getEntity());
                JsonNode root = objectMapper.readTree(json);
                
                JsonNode result = root.path("chart").path("result").get(0);
                if (result == null) {
                    return Optional.empty();
                }
                
                JsonNode meta = result.path("meta");
                JsonNode timestamps = result.path("timestamp");
                JsonNode indicators = result.path("indicators").path("quote").get(0);
                
                if (timestamps == null || timestamps.size() == 0) {
                    return Optional.empty();
                }
                
                int lastIndex = timestamps.size() - 1;
                long timestamp = timestamps.get(lastIndex).asLong();
                
                BigDecimal close = getBigDecimalValue(indicators.path("close").get(lastIndex));
                BigDecimal open = getBigDecimalValue(indicators.path("open").get(lastIndex));
                BigDecimal high = getBigDecimalValue(indicators.path("high").get(lastIndex));
                BigDecimal low = getBigDecimalValue(indicators.path("low").get(lastIndex));
                long volume = indicators.path("volume").get(lastIndex).asLong(0);
                
                GoldPrice price = GoldPrice.builder()
                        .symbol(symbol)
                        .timestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()))
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .build();
                
                log.info("成功获取价格 [{}]: {}", symbol, close);
                return Optional.of(price);
            }
        }
    }
    
    private BigDecimal getBigDecimalValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(node.asText()).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 保存价格数据
     */
    private void savePrice(GoldPrice price) {
        priceRepository.save(price);
        log.info("价格已保存: {} = {}", price.getSymbol(), price.getClose());
    }
    
    /**
     * 清理过期数据（保留90天）
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void cleanupOldData() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getCollector().getHistoryDays());
            priceRepository.deleteByTimestampBefore(cutoff);
            log.info("已清理{}天前的历史数据", config.getCollector().getHistoryDays());
        } catch (Exception e) {
            log.error("清理历史数据失败", e);
        }
    }
}
