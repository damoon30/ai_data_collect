package com.gold.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 黄金价格监控与投资建议系统
 */
@SpringBootApplication
@EnableScheduling
public class GoldMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoldMonitorApplication.class, args);
        System.out.println("""
            ╔═══════════════════════════════════════════════════╗
            ║                                                   ║
            ║     🏆 Gold Price Monitor & Advisor 🏆             ║
            ║     黄金价格监控与周度投资建议系统                 ║
            ║                                                   ║
            ║     功能:                                         ║
            ║     • 每5分钟抓取黄金价格                          ║
            ║     • 自动计算技术指标(MA/RSI/ATR)                ║
            ║     • 异常波动实时预警                            ║
            ║     • 每周日21:00生成投资建议                    ║
            ║                                                   ║
            ╚═══════════════════════════════════════════════════╝
            """);
    }
}
