package com.gold.monitor.pusher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gold.monitor.config.GoldMonitorConfig;
import com.gold.monitor.entity.TradingSignal;
import com.gold.monitor.repository.TradingSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书消息推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuPusher {
    
    private final GoldMonitorConfig config;
    private final TradingSignalRepository signalRepository;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    
    /**
     * 每分钟检查并推送未推送的信号
     */
    @Scheduled(fixedRate = 60000)
    public void pushPendingSignals() {
        if (!config.getPusher().getFeishu().isEnabled()) {
            return;
        }
        
        List<TradingSignal> pendingSignals = signalRepository.findByPushedFalse();
        
        for (TradingSignal signal : pendingSignals) {
            try {
                boolean success = pushSignal(signal);
                if (success) {
                    signal.setPushed(true);
                    signal.setPushTime(LocalDateTime.now());
                    signalRepository.save(signal);
                }
            } catch (Exception e) {
                log.error("推送信号失败: {}", signal.getId(), e);
            }
        }
    }
    
    /**
     * 推送单个信号
     */
    private boolean pushSignal(TradingSignal signal) {
        String webhookUrl = config.getPusher().getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("xxx")) {
            log.warn("飞书Webhook未配置，跳过推送");
            return false;
        }
        
        try {
            String content = buildMessageContent(signal);
            
            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "text");
            
            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            message.put("content", text);
            
            String jsonBody = objectMapper.writeValueAsString(message);
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(webhookUrl);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int statusCode = response.getCode();
                    String responseBody = EntityUtils.toString(response.getEntity());
                    
                    if (statusCode == 200) {
                        log.info("飞书推送成功: {}", signal.getSignalType());
                        return true;
                    } else {
                        log.error("飞书推送失败, 状态码: {}, 响应: {}", statusCode, responseBody);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.error("推送异常", e);
            return false;
        }
    }
    
    /**
     * 构建消息内容
     */
    private String buildMessageContent(TradingSignal signal) {
        StringBuilder sb = new StringBuilder();
        
        // 标题
        String levelEmoji = getLevelEmoji(signal.getSignalLevel());
        sb.append(levelEmoji).append(" ").append(signal.getDescription()).append("\n\n");
        
        // 当前价格
        sb.append("💰 当前价格: $").append(signal.getCurrentPrice()).append("\n");
        
        // 变动幅度
        if (signal.getChangePercent() != null) {
            String changeEmoji = signal.getDirection().equals("UP") ? "📈" : "📉";
            sb.append(changeEmoji).append(" 变动幅度: ")
              .append(signal.getChangePercent() > 0 ? "+" : "")
              .append(signal.getChangePercent()).append("%\n");
        }
        
        // 触发值
        if (signal.getTriggerValue() != null) {
            sb.append("🎯 触发价位: $").append(signal.getTriggerValue()).append("\n");
        }
        
        sb.append("\n");
        
        // 技术分析
        if (signal.getTechnicalAnalysis() != null) {
            sb.append("📊 技术分析:\n").append(signal.getTechnicalAnalysis()).append("\n\n");
        }
        
        // 风险提示
        if (signal.getRiskWarning() != null) {
            sb.append("⚠️ 风险提示:\n").append(signal.getRiskWarning()).append("\n\n");
        }
        
        // 免责声明
        sb.append("---\n");
        sb.append("🕐 ").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        sb.append("📌 本消息仅供参考，不构成投资建议");
        
        return sb.toString();
    }
    
    /**
     * 获取级别对应的emoji
     */
    private String getLevelEmoji(String level) {
        return switch (level) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            default -> "ℹ️";
        };
    }
    
    /**
     * 推送周度投资建议
     */
    public void pushWeeklyAdvice(String adviceContent) {
        String webhookUrl = config.getPusher().getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("xxx")) {
            log.warn("飞书Webhook未配置，跳过推送周度建议");
            return;
        }
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "text");
            
            Map<String, String> text = new HashMap<>();
            text.put("content", adviceContent);
            message.put("content", text);
            
            String jsonBody = objectMapper.writeValueAsString(message);
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost request = new HttpPost(webhookUrl);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
                
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (response.getCode() == 200) {
                        log.info("周度投资建议推送成功");
                    } else {
                        log.error("周度投资建议推送失败: {}", response.getCode());
                    }
                }
            }
        } catch (Exception e) {
            log.error("推送周度建议异常", e);
        }
    }
}
