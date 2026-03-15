package com.gold.monitor.advisor;

import com.gold.monitor.config.GoldMonitorConfig;
import com.gold.monitor.entity.GoldPrice;
import com.gold.monitor.entity.TechnicalIndicator;
import com.gold.monitor.pusher.FeishuPusher;
import com.gold.monitor.repository.GoldPriceRepository;
import com.gold.monitor.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 黄金投资建议生成器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentAdvisor {
    
    private final GoldMonitorConfig config;
    private final GoldPriceRepository priceRepository;
    private final TechnicalIndicatorRepository indicatorRepository;
    private final FeishuPusher feishuPusher;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 每周日 21:00 生成投资建议
     */
    @Scheduled(cron = "0 0 21 * * SUN")
    public void generateWeeklyAdvice() {
        try {
            log.info("开始生成周度投资建议...");
            
            Optional<GoldPrice> priceOpt = priceRepository.findTopByOrderByTimestampDesc();
            Optional<TechnicalIndicator> indicatorOpt = indicatorRepository.findTopByOrderByTimestampDesc();
            
            if (priceOpt.isEmpty() || indicatorOpt.isEmpty()) {
                log.warn("数据不足，无法生成投资建议");
                return;
            }
            
            GoldPrice price = priceOpt.get();
            TechnicalIndicator indicator = indicatorOpt.get();
            
            String advice = buildWeeklyAdvice(price, indicator);
            feishuPusher.pushWeeklyAdvice(advice);
            
            log.info("周度投资建议生成完成");
            
        } catch (Exception e) {
            log.error("生成周度投资建议异常", e);
        }
    }
    
    /**
     * 构建周度投资建议
     */
    private String buildWeeklyAdvice(GoldPrice price, TechnicalIndicator indicator) {
        // 计算下周日期范围
        LocalDateTime nextWeekStart = LocalDateTime.now().plusDays(1);
        LocalDateTime nextWeekEnd = nextWeekStart.plusDays(6);
        String weekRange = String.format("%s ~ %s", 
                nextWeekStart.format(FORMATTER), 
                nextWeekEnd.format(FORMATTER));
        
        // 判断趋势
        TrendView trend = analyzeTrend(indicator);
        VolatilityView volatility = analyzeVolatility(indicator);
        MacroView macro = analyzeMacroEnvironment();
        
        StringBuilder sb = new StringBuilder();
        
        // 标题
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║     📊 黄金下周投资建议 📊            ║\n");
        sb.append("╚══════════════════════════════════════╝\n\n");
        
        // 周期
        sb.append("📅 周期: ").append(weekRange).append("\n\n");
        
        // 当前价格
        sb.append("💰 当前价格: $").append(price.getClose()).append("\n");
        sb.append("📈 本周涨跌: ").append(indicator.getChangeWeek()).append("%\n");
        sb.append("📊 RSI(14): ").append(indicator.getRsi14()).append("\n\n");
        
        // 下周观点
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🎯 下周观点: ").append(trend.getEmoji()).append(" ").append(trend.getView()).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
        // 判断依据
        sb.append("📋 判断依据:\n\n");
        
        sb.append("1️⃣ 技术面:\n");
        sb.append("   • 价格与MA10关系: ").append(getMaRelation(price.getClose(), indicator.getMa10())).append("\n");
        sb.append("   • 价格与MA20关系: ").append(getMaRelation(price.getClose(), indicator.getMa20())).append("\n");
        sb.append("   • 本周趋势: ").append(trend.getDescription()).append("\n");
        sb.append("   • RSI状态: ").append(getRsiState(indicator.getRsi14())).append("\n\n");
        
        sb.append("2️⃣ 波动率:\n");
        sb.append("   • ATR(14): ").append(indicator.getAtr14()).append("\n");
        sb.append("   • 本周高低点: $").append(indicator.getWeekLow()).append(" ~ $").append(indicator.getWeekHigh()).append("\n");
        sb.append("   • 波动评估: ").append(volatility.getDescription()).append("\n\n");
        
        sb.append("3️⃣ 宏观环境:\n");
        sb.append("   ").append(macro.getDescription()).append("\n\n");
        
        // 建议仓位
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("💼 建议仓位:\n\n");
        
        var position = config.getAdvisor().getPosition();
        sb.append("   🟢 稳健型: ").append(position.getConservative().get(0)).append("%~").append(position.getConservative().get(1)).append("%\n");
        sb.append("   🟡 平衡型: ").append(position.getBalanced().get(0)).append("%~").append(position.getBalanced().get(1)).append("%\n");
        sb.append("   🔴 激进型: ").append(position.getAggressive().get(0)).append("%~").append(position.getAggressive().get(1)).append("%\n");
        sb.append("   ⚠️ 注意: 不建议超过50%，且需预留加仓空间\n\n");
        
        // 执行建议
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📝 执行建议:\n\n");
        
        sb.append("✅ 加仓条件:\n");
        sb.append("   • 价格回踩MA10附近并企稳\n");
        sb.append("   • 突破本周高点且成交量配合\n");
        sb.append("   • RSI从超卖区间(\u003c30)反弹\n\n");
        
        sb.append("❌ 减仓/离场条件:\n");
        sb.append("   • 价格跌破MA20\n");
        sb.append("   • 跌破本周低点且无法收回\n");
        sb.append("   • RSI进入严重超买(\u003e80)后拐头向下\n\n");
        
        // 止损建议
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🛡️ 止损建议:\n\n");
        
        var stopLoss = config.getAdvisor().getStopLoss();
        BigDecimal stopPrice = price.getClose().multiply(BigDecimal.valueOf(1 - stopLoss.getDefaultPercent() / 100));
        
        sb.append("   • 默认止损: ").append(stopLoss.getDefaultPercent()).append("%\n");
        sb.append("   • 严格止损: ").append(stopLoss.getTightPercent()).append("%\n");
        sb.append("   • 宽松止损: ").append(stopLoss.getLoosePercent()).append("%\n");
        sb.append("   • 当前价格参考止损位: $").append(stopPrice.setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n\n");
        
        // 风险提示
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("⚠️ 风险提示:\n\n");
        sb.append("   • 下周需关注宏观数据发布，可能放大波动\n");
        sb.append("   • 避免一次性重仓买入，建议分批建仓\n");
        sb.append("   • 严格设置止损，控制单笔亏损\n");
        sb.append("   • 重大事件前适当降低仓位\n\n");
        
        // 免责声明
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📌 免责声明:\n");
        sb.append("   本建议仅供参考，不构成投资承诺。\n");
        sb.append("   投资有风险，入市需谨慎。\n");
        sb.append("   请根据自身风险承受能力独立决策。\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        return sb.toString();
    }
    
    /**
     * 分析趋势
     */
    private TrendView analyzeTrend(TechnicalIndicator indicator) {
        BigDecimal price = indicator.getMa5();  // 用MA5代表当前价格趋势
        BigDecimal ma10 = indicator.getMa10();
        BigDecimal ma20 = indicator.getMa20();
        
        boolean aboveMa10 = price.compareTo(ma10) > 0;
        boolean aboveMa20 = price.compareTo(ma20) > 0;
        double weekChange = indicator.getChangeWeek().doubleValue();
        
        if (aboveMa10 && aboveMa20 && weekChange > 0) {
            return new TrendView("偏多", "📈", "价格位于MA10和MA20上方，本周上涨，趋势偏多");
        } else if (!aboveMa10 && !aboveMa20 && weekChange < 0) {
            return new TrendView("偏空", "📉", "价格跌破MA10和MA20，本周下跌，趋势偏空");
        } else if (aboveMa20) {
            return new TrendView("震荡偏多", "↗️", "价格在MA20上方但MA10附近震荡，短期方向不明");
        } else {
            return new TrendView("震荡", "↔️", "价格在均线附近震荡，等待方向选择");
        }
    }
    
    /**
     * 分析波动率
     */
    private VolatilityView analyzeVolatility(TechnicalIndicator indicator) {
        double atr = indicator.getAtr14().doubleValue();
        double price = indicator.getMa5().doubleValue();
        double atrPercent = (atr / price) * 100;
        
        if (atrPercent > 2.0) {
            return new VolatilityView("高波动", "波动率较高(>2%)，注意风险控制");
        } else if (atrPercent > 1.0) {
            return new VolatilityView("中等波动", "波动率中等，正常交易区间");
        } else {
            return new VolatilityView("低波动", "波动率较低，可能即将选择方向");
        }
    }
    
    /**
     * 分析宏观环境
     */
    private MacroView analyzeMacroEnvironment() {
        // 这里可以从外部API获取宏观数据，简化处理
        return new MacroView("宏观环境不确定性仍存，地缘风险对黄金形成支撑，但需警惕美联储政策变化带来的影响");
    }
    
    private String getMaRelation(BigDecimal price, BigDecimal ma) {
        if (price.compareTo(ma) > 0) {
            return "上方 (价差+$" + price.subtract(ma).setScale(2, BigDecimal.ROUND_HALF_UP) + ")";
        } else {
            return "下方 (价差-$" + ma.subtract(price).setScale(2, BigDecimal.ROUND_HALF_UP) + ")";
        }
    }
    
    private String getRsiState(BigDecimal rsi) {
        double value = rsi.doubleValue();
        if (value > 70) return "超买区(>70)，注意回调";
        if (value > 50) return "强势区(50-70)";
        if (value > 30) return "弱势区(30-50)";
        return "超卖区(<30)，关注反弹";
    }
    
    // 内部类用于视图
    private record TrendView(String view, String emoji, String description) {}
    private record VolatilityView(String level, String description) {}
    private record MacroView(String description) {}
}
