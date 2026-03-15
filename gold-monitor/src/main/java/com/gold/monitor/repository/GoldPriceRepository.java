package com.gold.monitor.repository;

import com.gold.monitor.entity.GoldPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {
    
    /**
     * 查询最新价格
     */
    Optional<GoldPrice> findTopByOrderByTimestampDesc();
    
    /**
     * 查询指定时间范围内的价格
     */
    List<GoldPrice> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end);
    
    /**
     * 查询最近N条记录
     */
    List<GoldPrice> findTopNByOrderByTimestampDesc(int limit);
    
    /**
     * 查询指定时间之前的价格（用于计算涨跌幅）
     */
    @Query("SELECT g FROM GoldPrice g WHERE g.timestamp <= :time ORDER BY g.timestamp DESC LIMIT 1")
    Optional<GoldPrice> findPriceAtOrBefore(@Param("time") LocalDateTime time);
    
    /**
     * 查询指定天数内的数据
     */
    @Query("SELECT g FROM GoldPrice g WHERE g.timestamp >= :startTime ORDER BY g.timestamp ASC")
    List<GoldPrice> findRecentData(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 删除N天前的数据
     */
    void deleteByTimestampBefore(LocalDateTime time);
}
