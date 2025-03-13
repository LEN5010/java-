package com.quanttrading;

import com.quanttrading.datasource.DataSource;
import com.quanttrading.factory.DataSourceFactory;
import com.quanttrading.factory.PreprocessorFactory;
import com.quanttrading.factory.StrategyFactory;
import com.quanttrading.model.ProcessedData;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;
import com.quanttrading.preprocessing.DataPreprocessor;
import com.quanttrading.strategy.TradingStrategy;
import com.quanttrading.visualization.ChartGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TradingSystem {
    private static final Logger logger = LoggerFactory.getLogger(TradingSystem.class);
    private static final String CONFIG_FILE = "src/main/resources/config.properties";

    public static void main(String[] args) {
        try {
            // 1. 加载配置
            Properties props = loadProperties();

            // 2. 初始化数据源
            DataSource dataSource = initDataSource(props);

            // 3. 初始化预处理器
            DataPreprocessor preprocessor = initPreprocessor(props);

            // 4. 初始化交易策略
            TradingStrategy strategy = initStrategy(props);

            // 5. 获取交易参数
            String symbol = props.getProperty("trading.symbol", "AAPL");
            LocalDate startDate = LocalDate.parse(
                    props.getProperty("trading.start_date", "2023-01-01"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate endDate = LocalDate.parse(
                    props.getProperty("trading.end_date", "2023-12-31"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 6. 获取原始数据
            logger.info("Fetching historical data for {} from {} to {}", symbol, startDate, endDate);
            List<StockData> rawData = dataSource.fetchHistoricalData(symbol, startDate, endDate);
            logger.info("Retrieved {} data points", rawData.size());

            // 7. 数据预处理
            List<ProcessedData> processedData = preprocessor.process(rawData);

            // 8. 计算移动平均线
            int shortWindow = Integer.parseInt(props.getProperty("ma.short_window", "5"));
            int longWindow = Integer.parseInt(props.getProperty("ma.long_window", "20"));

            List<Double> shortMAList = calculateMovingAverages(rawData, shortWindow);
            List<Double> longMAList = calculateMovingAverages(rawData, longWindow);

            // 9. 创建指标映射
            Map<String, List<Double>> indicators = new HashMap<>();
            indicators.put("Short MA (" + shortWindow + ")", shortMAList);
            indicators.put("Long MA (" + longWindow + ")", longMAList);

            // 10. 执行交易策略并收集信号
            Map<Date, TradeSignal> signals = new HashMap<>();

            for (int i = 0; i < processedData.size(); i++) {
                ProcessedData current = processedData.get(i);
                // 使用当前数据点之前的历史数据生成信号
                List<ProcessedData> history = processedData.subList(
                        Math.min(i + 1, processedData.size() - 1),
                        processedData.size());

                TradeSignal signal = strategy.generateSignal(current, history);

                if (signal != TradeSignal.HOLD) {
                    Date date = Date.from(current.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    signals.put(date, signal);
                }

                logger.info("{}: {} - Signal: {}", current.getDate(), rawData.get(i).getClose(), signal);
            }

            // 11. 显示图表
            boolean showChart = Boolean.parseBoolean(props.getProperty("visualization.show_chart", "true"));
            if (showChart) {
                logger.info("Generating price chart with indicators and signals...");

                String chartType = props.getProperty("visualization.chart_type", "line");
                String chartTitle = symbol + " Price Chart (" + startDate + " to " + endDate + ")";

                if ("candlestick".equalsIgnoreCase(chartType)) {
                    ChartGenerator.createCandlestickChart(chartTitle, rawData, indicators, signals);
                } else {
                    ChartGenerator.createPriceMAChart(chartTitle, rawData, indicators, signals);
                }
            }

            // 12. 保存图表（可选）
            boolean saveChart = Boolean.parseBoolean(props.getProperty("visualization.save_chart", "false"));
            if (saveChart) {
                String filePath = props.getProperty("visualization.save_path", "chart.png");
                // 图表保存逻辑将在ChartGenerator中实现
            }

        } catch (Exception e) {
            logger.error("Error in trading system: {}", e.getMessage(), e);
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        }
        return props;
    }

    private static DataSource initDataSource(Properties props) {
        String type = props.getProperty("datasource.type", "yahoo");
        DataSource dataSource = DataSourceFactory.createDataSource(type);

        Map<String, Object> config = new HashMap<>();
        if (type.equals("yahoo")) {
            config.put("timeout", Integer.parseInt(props.getProperty("yahoo.api.timeout", "5000")));
        } else if (type.equals("local")) {
            config.put("dataDirectory", props.getProperty("local.data.directory"));
            config.put("dateFormat", props.getProperty("local.date.format"));
        }

        dataSource.configure(config);
        return dataSource;
    }

    private static DataPreprocessor initPreprocessor(Properties props) {
        String type = props.getProperty("preprocessor.type", "standardization");
        DataPreprocessor preprocessor = PreprocessorFactory.createPreprocessor(type);

        Map<String, Double> params = new HashMap<>();
        // 添加预处理器参数（如果有）

        preprocessor.setParameters(params);
        return preprocessor;
    }

    private static TradingStrategy initStrategy(Properties props) {
        String type = props.getProperty("strategy.type", "moving_average");
        TradingStrategy strategy = StrategyFactory.createStrategy(type);

        Map<String, Double> params = new HashMap<>();
        if (type.equals("moving_average") || type.equals("ma")) {
            params.put("shortWindow", Double.parseDouble(props.getProperty("ma.short_window", "5")));
            params.put("longWindow", Double.parseDouble(props.getProperty("ma.long_window", "20")));
        }

        strategy.setParameters(params);
        return strategy;
    }

    private static List<Double> calculateMovingAverages(List<StockData> data, int window) {
        List<Double> result = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            double sum = 0;
            int count = 0;

            // 计算过去window天的平均值
            for (int j = Math.max(0, i - window + 1); j <= i; j++) {
                sum += data.get(j).getClose();
                count++;
            }

            result.add(sum / count);
        }

        return result;
    }
}