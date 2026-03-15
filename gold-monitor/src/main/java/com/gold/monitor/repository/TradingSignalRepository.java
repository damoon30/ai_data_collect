package com.gold.monitor.repository;

import com.gold.monitor.entity.TradingSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradingSignalRepository extends JpaRepository<TradingSignal, Long> {
    
    /**
     * 查询未推送的信号
     */
    List<TradingSignal> findByPushedFalse();
    
    /**
     * 查询指定时间内的信号
     */
    List<TradingSignal> findByCreatedAtAfterAndSignalType(
            LocalDateTime time, String signalType);
    
    /**
     * 查询最近N条信号
     */
    List<TradingSignal> findTop10ByOrderByCreatedAtDesc();
}
