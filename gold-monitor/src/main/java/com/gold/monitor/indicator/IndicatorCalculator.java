package com.gold.monitor.indicator;

import com.gold.monitor.config.GoldMonitorConfig;
import com.gold.monitor.entity.GoldPrice;
import com.gold.monitor.entity.TechnicalIndicator;
import com.gold.monitor.repository.GoldPriceRepository;
import com.gold.monitor.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 技术指标计算服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorCalculator {
    
    private final GoldMonitorConfig config;
    private final GoldPriceRepository priceRepository;
    private final TechnicalIndicatorRepository indicatorRepository;
    
    /**
     * 每5分钟计算一次指标
     */
    @Scheduled(fixedRateString = "${gold.monitor.collector.interval-minutes:5}00000")
    public void calculate() {
        try {
            List<GoldPrice> prices = getRecentPrices(100);
            if (prices.size() < 20) {
                log.warn("数据不足，无法计算指标，当前数据条数: {}", prices.size());
                return;
            }
            
            BarSeries series = createBarSeries(prices);
            GoldPrice latestPrice = prices.get(0);
            
            TechnicalIndicator indicator = calculateIndicators(series, latestPrice);
            indicatorRepository.save(indicator);
            
            log.info("技术指标计算完成: MA5={}, MA10={}, MA20={}, RSI14={}",
                    indicator.getMa5(), indicator.getMa10(), indicator.getMa20(), indicator.getRsi14());
            
        } catch (Exception e) {
            log.error("指标计算异常", e);
        }
    }
    
    /**
     * 获取最近N条价格数据
     */
    private List<GoldPrice> getRecentPrices(int limit) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        return priceRepository.findRecentData(startTime);
    }
    
    /**
     * 创建K线序列
     */
    private BarSeries createBarSeries(List<GoldPrice> prices) {
        BarSeries series = new BaseBarSeriesBuilder().withName("XAUUSD").build();
        
        for (int i = prices.size() - 1; i >= 0; i--) {
            GoldPrice p = prices.get(i);
            Bar bar = BaseBar.builder()
                    .timePeriod(Duration.ofMinutes(5))
                    .endTime(p.getTimestamp().atZone(ZoneId.systemDefault()))
                    .openPrice(p.getOpen().doubleValue())
                    .highPrice(p.getHigh().doubleValue())
                    .lowPrice(p.getLow().doubleValue())
                    .closePrice(p.getClose().doubleValue())
                    .volume(p.getVolume().doubleValue())
                    .build();
            series.addBar(bar);
        }
        
        return series;
    }
    
    /**
     * 计算技术指标
     */
    private TechnicalIndicator calculateIndicators(BarSeries series, GoldPrice latestPrice) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        
        // 计算移动平均线
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        
        // 计算RSI
        RSIIndicator rsi = new RSIIndicator(closePrice, config.getIndicator().getRsiPeriod());
        
        // 计算ATR
        ATRIndicator atr = new ATRIndicator(series, config.getIndicator().getAtrPeriod());
        
        // 计算涨跌幅
        BigDecimal change5m = calculateChange(series, 1);      // 5分钟（1个5分钟K线）
        BigDecimal change1h = calculateChange(series, 12);     // 1小时（12个5分钟K线）
        BigDecimal change1d = calculateChange(series, 288);    // 1天（288个5分钟K线，假设24小时交易）
        BigDecimal changeWeek = calculateWeekChange(series);
        
        // 获取高低点
        BigDecimal dayHigh = calculateDayHigh(series);
        BigDecimal dayLow = calculateDayLow(series);
        BigDecimal weekHigh = calculateWeekHigh(series);
        BigDecimal weekLow = calculateWeekLow(series);
        
        int lastIndex = series.getEndIndex();
        
        return TechnicalIndicator.builder()
                .priceId(latestPrice.getId())
                .timestamp(latestPrice.getTimestamp())
                .ma5(toBigDecimal(sma5.getValue(lastIndex)))
                .ma10(toBigDecimal(sma10.getValue(lastIndex)))
                .ma20(toBigDecimal(sma20.getValue(lastIndex)))
                .rsi14(toBigDecimal(rsi.getValue(lastIndex)))
                .atr14(toBigDecimal(atr.getValue(lastIndex)))
                .change5m(change5m)
                .change1h(change1h)
                .change1d(change1d)
                .changeWeek(changeWeek)
                .dayHigh(dayHigh)
                .dayLow(dayLow)
                .weekHigh(weekHigh)
                .weekLow(weekLow)
                .build();
    }
    
    /**
     * 计算涨跌幅
     */
    private BigDecimal calculateChange(BarSeries series, int periods) {
        int lastIndex = series.getEndIndex();
        int prevIndex = Math.max(0, lastIndex - periods);
        
        if (prevIndex == lastIndex) return BigDecimal.ZERO;
        
        double current = series.getBar(lastIndex).getClosePrice().doubleValue();
        double previous = series.getBar(prevIndex).getClosePrice().doubleValue();
        
        return BigDecimal.valueOf((current - previous) / previous * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算本周涨跌幅
     */
    private BigDecimal calculateWeekChange(BarSeries series) {
        // 简化处理：取最早和最晚的价格计算
        int lastIndex = series.getEndIndex();
        int firstIndex = Math.max(0, lastIndex - 2016); // 假设一周约2016个5分钟K线
        
        double current = series.getBar(lastIndex).getClosePrice().doubleValue();
        double start = series.getBar(firstIndex).getClosePrice().doubleValue();
        
        return BigDecimal.valueOf((current - start) / start * 100)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算日内高点
     */
    private BigDecimal calculateDayHigh(BarSeries series) {
        int lastIndex = series.getEndIndex();
        int startIndex = Math.max(0, lastIndex - 288); // 约一天
        
        double high = Double.MIN_VALUE;
        for (int i = startIndex; i <= lastIndex; i++) {
            high = Math.max(high, series.getBar(i).getHighPrice().doubleValue());
        }
        
        return BigDecimal.valueOf(high).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算日内低点
     */
    private BigDecimal calculateDayLow(BarSeries series) {
        int lastIndex = series.getEndIndex();
        int startIndex = Math.max(0, lastIndex - 288);
        
        double low = Double.MAX_VALUE;
        for (int i = startIndex; i <= lastIndex; i++) {
            low = Math.min(low, series.getBar(i).getLowPrice().doubleValue());
        }
        
        return BigDecimal.valueOf(low).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算本周高点
     */
    private BigDecimal calculateWeekHigh(BarSeries series) {
        int lastIndex = series.getEndIndex();
        int startIndex = Math.max(0, lastIndex - 2016);
        
        double high = Double.MIN_VALUE;
        for (int i = startIndex; i <= lastIndex; i++) {
            high = Math.max(high, series.getBar(i).getHighPrice().doubleValue());
        }
        
        return BigDecimal.valueOf(high).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算本周低点
     */
    private BigDecimal calculateWeekLow(BarSeries series) {
        int lastIndex = series.getEndIndex();
        int startIndex = Math.max(0, lastIndex - 2016);
        
        double low = Double.MAX_VALUE;
        for (int i = startIndex; i <= lastIndex; i++) {
            low = Math.min(low, series.getBar(i).getLowPrice().doubleValue());
        }
        
        return BigDecimal.valueOf(low).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 检查均线金叉/死叉
     */
    public boolean isGoldenCross(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        
        CrossedUpIndicatorRule goldenCross = new CrossedUpIndicatorRule(sma5, sma10);
        return goldenCross.isSatisfied(series.getEndIndex());
    }
    
    /**
     * 检查均线死叉
     */
    public boolean isDeathCross(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma10 = new SMAIndicator(closePrice, 10);
        
        CrossedDownIndicatorRule deathCross = new CrossedDownIndicatorRule(sma5, sma10);
        return deathCross.isSatisfied(series.getEndIndex());
    }
    
    private BigDecimal toBigDecimal(org.ta4j.core.num.Num num) {
        return BigDecimal.valueOf(num.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }
}
