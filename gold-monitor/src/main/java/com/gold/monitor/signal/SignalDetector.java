package com.gold.monitor.signal;

import com.gold.monitor.config.GoldMonitorConfig;
import com.gold.monitor.entity.GoldPrice;
import com.gold.monitor.entity.TechnicalIndicator;
import com.gold.monitor.entity.TradingSignal;
import com.gold.monitor.repository.GoldPriceRepository;
import com.gold.monitor.repository.TechnicalIndicatorRepository;
import com.gold.monitor.repository.TradingSignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 交易信号检测器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalDetector {
    
    private final GoldMonitorConfig config;
    private final GoldPriceRepository priceRepository;
    private final TechnicalIndicatorRepository indicatorRepository;
    private final TradingSignalRepository signalRepository;
    
    /**
     * 每5分钟检测一次信号
     */
    @Scheduled(fixedRateString = "${gold.monitor.collector.interval-minutes:5}00000")
    public void detect() {
        try {
            Optional<GoldPrice> priceOpt = priceRepository.findTopByOrderByTimestampDesc();
            Optional<TechnicalIndicator> indicatorOpt = indicatorRepository.findTopByOrderByTimestampDesc();
            
            if (priceOpt.isEmpty() || indicatorOpt.isEmpty()) {
                return;
            }
            
            GoldPrice price = priceOpt.get();
            TechnicalIndicator indicator = indicatorOpt.get();
            
            // 检查涨跌幅异常
            checkPriceChange(price, indicator);
            
            // 检查突破
            checkBreakout(price, indicator);
            
            // 检查均线穿越
            checkMaCross(price, indicator);
            
        } catch (Exception e) {
            log.error("信号检测异常", e);
        }
    }
    
    /**
     * 检查价格涨跌幅异常
     */
    private void checkPriceChange(GoldPrice price, TechnicalIndicator indicator) {
        double threshold5m = config.getSignal().getChangeThreshold().getMinute5();
        double threshold1h = config.getSignal().getChangeThreshold().getHour1();
        
        // 5分钟异动
        if (indicator.getChange5m().abs().doubleValue() > threshold5m) {
            String direction = indicator.getChange5m().doubleValue() > 0 ? "UP" : "DOWN";
            String emoji = direction.equals("UP") ? "🚀" : "📉";
            
            TradingSignal signal = TradingSignal.builder()
                    .signalType("PRICE_CHANGE_5M")
                    .signalLevel("WARNING")
                    .currentPrice(price.getClose())
                    .changePercent(indicator.getChange5m())
                    .direction(direction)
                    .description(String.format("%s 黄金价格5分钟%s %.2f%%", 
                            emoji, 
                            direction.equals("UP") ? "上涨" : "下跌",
                            indicator.getChange5m().abs().doubleValue()))
                    .technicalAnalysis(String.format("当前价格: $%s, 5分钟%s %.2f%%, 超过阈值 %.1f%%",
                            price.getClose(),
                            direction.equals("UP") ? "涨幅" : "跌幅",
                            indicator.getChange5m().abs().doubleValue(),
                            threshold5m))
                    .riskWarning("短线波动加剧，建议谨慎操作，注意止损")
                    .build();
            
            saveSignal(signal);
        }
        
        // 1小时异动
        if (indicator.getChange1h().abs().doubleValue() > threshold1h) {
            String direction = indicator.getChange1h().doubleValue() > 0 ? "UP" : "DOWN";
            String emoji = direction.equals("UP") ? "🔥" : "❄️";
            
            TradingSignal signal = TradingSignal.builder()
                    .signalType("PRICE_CHANGE_1H")
                    .signalLevel("CRITICAL")
                    .currentPrice(price.getClose())
                    .changePercent(indicator.getChange1h())
                    .direction(direction)
                    .description(String.format("%s 黄金价格1小时%s %.2f%%", 
                            emoji,
                            direction.equals("UP") ? "大幅上涨" : "大幅下跌",
                            indicator.getChange1h().abs().doubleValue()))
                    .technicalAnalysis(String.format("当前价格: $%s, 1小时%s %.2f%%, 超过阈值 %.1f%%. " +
                            "RSI(14)=%s, 市场%s",
                            price.getClose(),
                            direction.equals("UP") ? "涨幅" : "跌幅",
                            indicator.getChange1h().abs().doubleValue(),
                            threshold1h,
                            indicator.getRsi14(),
                            getRsiDescription(indicator.getRsi14())))
                    .riskWarning(direction.equals("UP") ? 
                            "涨速过快，谨防冲高回落，建议分批减仓" : 
                            "跌速过快，可能超卖，但不要轻易抄底，等待企稳信号")
                    .build();
            
            saveSignal(signal);
        }
    }
    
    /**
     * 检查突破信号
     */
    private void checkBreakout(GoldPrice price, TechnicalIndicator indicator) {
        if (!config.getSignal().getBreakout().isEnableDayHighLow()) {
            return;
        }
        
        // 突破日内高点
        if (price.getClose().compareTo(indicator.getDayHigh()) >= 0) {
            TradingSignal signal = TradingSignal.builder()
                    .signalType("BREAKOUT_DAY_HIGH")
                    .signalLevel("INFO")
                    .currentPrice(price.getClose())
                    .triggerValue(indicator.getDayHigh())
                    .direction("UP")
                    .description("📈 黄金价格突破日内高点")
                    .technicalAnalysis(String.format("当前价格 $%s 突破日内高点 $%s, 显示多头强势",
                            price.getClose(), indicator.getDayHigh()))
                    .riskWarning("突破需确认有效性，防止假突破回落，建议观察成交量")
                    .build();
            
            saveSignal(signal);
        }
        
        // 跌破日内低点
        if (price.getClose().compareTo(indicator.getDayLow()) <= 0) {
            TradingSignal signal = TradingSignal.builder()
                    .signalType("BREAKDOWN_DAY_LOW")
                    .signalLevel("WARNING")
                    .currentPrice(price.getClose())
                    .triggerValue(indicator.getDayLow())
                    .direction("DOWN")
                    .description("📉 黄金价格跌破日内低点")
                    .technicalAnalysis(String.format("当前价格 $%s 跌破日内低点 $%s, 显示空头占优",
                            price.getClose(), indicator.getDayLow()))
                    .riskWarning("跌破支撑可能引发进一步下跌，建议减仓或观望")
                    .build();
            
            saveSignal(signal);
        }
    }
    
    /**
     * 检查均线穿越
     */
    private void checkMaCross(GoldPrice price, TechnicalIndicator indicator) {
        if (!config.getSignal().getBreakout().isEnableMaCross()) {
            return;
        }
        
        // 价格站上MA10
        if (price.getClose().compareTo(indicator.getMa10()) > 0 && 
            price.getOpen().compareTo(indicator.getMa10()) <= 0) {
            
            TradingSignal signal = TradingSignal.builder()
                    .signalType("MA10_CROSS_UP")
                    .signalLevel("INFO")
                    .currentPrice(price.getClose())
                    .triggerValue(indicator.getMa10())
                    .direction("UP")
                    .description("📊 黄金价格站上MA10")
                    .technicalAnalysis(String.format("价格 $%s 站上MA10 $%s, 短期趋势转多",
                            price.getClose(), indicator.getMa10()))
                    .riskWarning("MA10只是短期均线，需结合MA20和趋势判断，避免频繁交易")
                    .build();
            
            saveSignal(signal);
        }
        
        // 价格跌破MA10
        if (price.getClose().compareTo(indicator.getMa10()) < 0 &&
            price.getOpen().compareTo(indicator.getMa10()) >= 0) {
            
            TradingSignal signal = TradingSignal.builder()
                    .signalType("MA10_CROSS_DOWN")
                    .signalLevel("WARNING")
                    .currentPrice(price.getClose())
                    .triggerValue(indicator.getMa10())
                    .direction("DOWN")
                    .description("📊 黄金价格跌破MA10")
                    .technicalAnalysis(String.format("价格 $%s 跌破MA10 $%s, 短期趋势转空",
                            price.getClose(), indicator.getMa10()))
                    .riskWarning("跌破短期均线，可能是回调信号，也可能是趋势反转，需观察MA20支撑")
                    .build();
            
            saveSignal(signal);
        }
        
        // 价格跌破MA20（更严重的信号）
        if (price.getClose().compareTo(indicator.getMa20()) < 0 &&
            price.getOpen().compareTo(indicator.getMa20()) >= 0) {
            
            TradingSignal signal = TradingSignal.builder()
                    .signalType("MA20_CROSS_DOWN")
                    .signalLevel("CRITICAL")
                    .currentPrice(price.getClose())
                    .triggerValue(indicator.getMa20())
                    .direction("DOWN")
                    .description("⚠️ 黄金价格跌破MA20")
                    .technicalAnalysis(String.format("价格 $%s 跌破MA20 $%s, 中期趋势可能转空",
                            price.getClose(), indicator.getMa20()))
                    .riskWarning("跌破中期均线，建议减仓或止损，等待趋势明朗后再入场")
                    .build();
            
            saveSignal(signal);
        }
    }
    
    /**
     * 保存信号（带冷却期检查）
     */
    private void saveSignal(TradingSignal signal) {
        // 检查冷却期
        LocalDateTime cooldownTime = LocalDateTime.now().minusMinutes(config.getSignal().getCooldownMinutes());
        var recentSignals = signalRepository.findByCreatedAtAfterAndSignalType(cooldownTime, signal.getSignalType());
        
        if (!recentSignals.isEmpty()) {
            log.info("信号类型 {} 在冷却期内，跳过推送", signal.getSignalType());
            return;
        }
        
        signalRepository.save(signal);
        log.info("检测到交易信号: {} - {}", signal.getSignalType(), signal.getDescription());
    }
    
    /**
     * 获取RSI描述
     */
    private String getRsiDescription(BigDecimal rsi) {
        double value = rsi.doubleValue();
        if (value > 70) return "超买区间，注意回调风险";
        if (value < 30) return "超卖区间，可能存在反弹机会";
        return "中性区间";
    }
}
