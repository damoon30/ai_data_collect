package com.gold.monitor.repository;

import com.gold.monitor.entity.TechnicalIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicator, Long> {
    
    /**
     * 查询最新指标
     */
    Optional<TechnicalIndicator> findTopByOrderByTimestampDesc();
    
    /**
     * 删除N天前的数据
     */
    void deleteByTimestampBefore(LocalDateTime time);
}
