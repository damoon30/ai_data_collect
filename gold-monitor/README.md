# 🏆 Gold Price Monitor & Advisor

黄金价格监控与周度投资建议系统

## 📋 项目简介

本项目是一个基于 Java 17 + Spring Boot 3 的黄金价格实时监控与智能投资建议系统，具备以下核心功能：

- ✅ 每5分钟抓取黄金价格（XAUUSD优先，COMEX备选）
- ✅ 自动计算技术指标（MA5/MA10/MA20、RSI14、ATR14）
- ✅ 实时监控异常波动并推送飞书消息
- ✅ 每周日21:00自动生成投资建议报告
- ✅ 模块化设计，支持YAML配置
- ✅ 支持失败重试、日志记录、异常告警

## 🏗️ 技术架构

```
gold-price-monitor/
├── pom.xml                              # Maven配置
├── src/
│   └── main/
│       ├── java/com/gold/monitor/
│       │   ├── GoldMonitorApplication.java    # 启动类
│       │   ├── collector/
│       │   │   └── PriceCollector.java        # 价格采集器
│       │   ├── indicator/
│       │   │   └── IndicatorCalculator.java   # 指标计算器
│       │   ├── signal/
│       │   │   └── SignalDetector.java        # 信号检测器
│       │   ├── advisor/
│       │   │   └── InvestmentAdvisor.java     # 投资建议生成器
│       │   ├── pusher/
│       │   │   └── FeishuPusher.java          # 飞书推送器
│       │   ├── config/
│       │   │   └── GoldMonitorConfig.java     # 配置类
│       │   ├── entity/
│       │   │   ├── GoldPrice.java             # 价格实体
│       │   │   ├── TechnicalIndicator.java    # 指标实体
│       │   │   └── TradingSignal.java         # 信号实体
│       │   └── repository/
│       │       ├── GoldPriceRepository.java
│       │       ├── TechnicalIndicatorRepository.java
│       │       └── TradingSignalRepository.java
│       └── resources/
│           └── application.yml          # 配置文件
└── README.md
```

## 🚀 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.8+
- H2 Database（内置，无需额外安装）

### 2. 克隆项目

```bash
git clone https://github.com/damoon30/ai_data_collect.git
cd ai_data_collect/gold-monitor
```

### 3. 配置飞书Webhook

编辑 `src/main/resources/application.yml`：

```yaml
gold:
  monitor:
    pusher:
      feishu:
        enabled: true
        webhook-url: "https://open.feishu.cn/open-apis/bot/v2/hook/YOUR_WEBHOOK_TOKEN"
        chat-id: "oc_df910cb1db23f0e3ea4f9a9392888cd8"
```

获取飞书Webhook：
1. 在飞书群中添加自定义机器人
2. 复制Webhook地址替换上述配置

### 4. 编译运行

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/gold-price-monitor-1.0.0.jar
```

或本地开发运行：

```bash
mvn spring-boot:run
```

### 5. 访问H2控制台（可选）

启动后访问：http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:file:./data/golddb`
- 用户名: `sa`
- 密码: 空

## ⚙️ 配置说明

### 核心配置项

```yaml
gold:
  monitor:
    # 价格源配置
    price-source:
      primary: "XAUUSD"      # 主数据源
      backup: "GC=F"         # 备选数据源
      api-url: "https://query1.finance.yahoo.com/v8/finance/chart/"
    
    # 采集配置
    collector:
      interval-minutes: 5    # 采集间隔（分钟）
      retry-times: 3         # 失败重试次数
      retry-delay-ms: 5000   # 重试间隔（毫秒）
      history-days: 90       # 历史数据保留天数
    
    # 信号监控阈值
    signal:
      change-threshold:
        minute5: 0.5         # 5分钟异动阈值(%)
        hour1: 1.2           # 1小时异动阈值(%)
        day1: 2.0            # 单日异动阈值(%)
      cooldown-minutes: 15   # 信号冷却期（避免重复推送）
    
    # 投资建议仓位配置
    advisor:
      position:
        conservative: [10, 20]    # 稳健型: 10%-20%
        balanced: [20, 35]        # 平衡型: 20%-35%
        aggressive: [30, 50]      # 激进型: 30%-50%
      stop-loss:
        default-percent: 3.0      # 默认止损: 3%
```

## 📊 功能详解

### 1. 价格采集 (collector)

- 每5分钟从Yahoo Finance获取XAUUSD价格
- 失败自动重试3次，自动切换到COMEX备选源
- 数据保存到H2数据库，保留90天历史

### 2. 指标计算 (indicator)

基于TA4J库计算：
- **MA5/MA10/MA20**: 移动平均线
- **RSI(14)**: 相对强弱指标
- **ATR(14)**: 平均真实波幅
- **涨跌幅**: 5分钟/1小时/1天/本周
- **高低点**: 日内/本周高低点

### 3. 信号检测 (signal)

监控以下异常并推送飞书：

| 信号类型 | 触发条件 | 级别 |
|---------|---------|------|
| PRICE_CHANGE_5M | 5分钟涨跌超0.5% | WARNING |
| PRICE_CHANGE_1H | 1小时涨跌超1.2% | CRITICAL |
| BREAKOUT_DAY_HIGH | 突破日内高点 | INFO |
| BREAKDOWN_DAY_LOW | 跌破日内低点 | WARNING |
| MA10_CROSS_UP | 站上MA10 | INFO |
| MA10_CROSS_DOWN | 跌破MA10 | WARNING |
| MA20_CROSS_DOWN | 跌破MA20 | CRITICAL |

### 4. 投资建议 (advisor)

每周日21:00自动生成报告，包括：
- 下周观点（偏多/偏空/震荡）
- 技术面分析（趋势、RSI状态）
- 波动率评估
- 建议仓位（按风险偏好分级）
- 加/减仓条件
- 止损建议
- 风险提示

### 5. 消息推送 (pusher)

- 异常信号实时推送（带15分钟冷却期）
- 周度投资建议定时推送
- 包含当前价格、技术分析、风险提示

## 🧪 自测说明

### 1. 单元测试

```bash
mvn test
```

### 2. 本地验证

1. 启动应用后查看日志输出价格采集
2. 访问H2控制台查看数据是否写入
3. 修改阈值触发信号测试推送功能

### 3. 手动触发周度建议

```bash
# 使用curl或postman调用
POST http://localhost:8080/api/advice/generate
```

（需要在Controller中添加对应端点）

## 📁 数据存储

数据保存在 `./data/golddb.mv.db`：

| 表名 | 说明 |
|------|------|
| gold_price | 原始价格数据 |
| technical_indicator | 技术指标数据 |
| trading_signal | 交易信号记录 |

## 🔒 风控声明

⚠️ **重要提示**：

1. 本系统提供的所有信息**仅供参考，不构成投资建议**
2. 不建议满仓操作，系统最大建议仓位不超过50%
3. 每次输出都包含止损条件，请严格执行
4. 投资有风险，入市需谨慎，请根据自身情况独立决策
5. 重大宏观事件前系统会自动提示降低仓位

## 📝 日志查看

```bash
# 实时查看日志
tail -f logs/gold-monitor.log

# 查看最近100行
tail -n 100 logs/gold-monitor.log
```

## 🛠️ 开发计划

- [ ] 接入更多数据源（Bloomberg、Wind等）
- [ ] 增加机器学习预测模型
- [ ] 支持多品种监控（白银、原油等）
- [ ] 增加邮件/短信通知渠道
- [ ] 开发Web管理界面

## 📄 License

MIT License

## 🤝 贡献

欢迎提交Issue和PR！

---

**免责声明**：本项目仅供学习交流使用，不构成任何投资建议。投资者应独立做出投资决策并承担风险。
