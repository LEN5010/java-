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
import com.quanttrading.ml.MachineLearningAlgorithm;
import com.quanttrading.ml.factory.MLAlgorithmFactory;
import com.quanttrading.strategy.impl.MachineLearningStrategy;
import com.quanttrading.evaluation.StrategyEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 量化交易系统主类
 */
public class TradingSystem {
    private static final Logger logger = LoggerFactory.getLogger(TradingSystem.class);
    private static final String CONFIG_FILE = "src/main/resources/config.properties";

    public static void main(String[] args) {
        try {
            // 1. 加载配置
            Properties props = loadProperties();

            // 2. 获取交易参数
            String symbol = props.getProperty("trading.symbol", "AAPL");
            LocalDate startDate = LocalDate.parse(
                    props.getProperty("trading.start_date", "2023-01-01"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate endDate = LocalDate.parse(
                    props.getProperty("trading.end_date", "2023-12-31"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 3. 初始化数据源
            DataSource dataSource = initDataSource(props);

            // 4. 获取原始数据
            logger.info("Fetching historical data for {} from {} to {}", symbol, startDate, endDate);
            List<StockData> rawData = dataSource.fetchHistoricalData(symbol, startDate, endDate);
            logger.info("Retrieved {} data points", rawData.size());

            // 5. 初始化预处理器
            DataPreprocessor preprocessor = initPreprocessor(props);

            // 6. 数据预处理
            List<ProcessedData> processedData = preprocessor.process(rawData);

            // 7. 策略选择
            String strategyType = props.getProperty("strategy.type", "moving_average");
            Map<LocalDate, TradeSignal> signals = new HashMap<>();

            if ("ml".equals(strategyType) || "machine_learning".equals(strategyType)) {
                // 使用机器学习策略
                signals = executeMLStrategy(props, rawData, processedData);
            } else {
                // 使用传统策略
                signals = executeTraditionalStrategy(props, rawData, processedData);
            }

            // 8. 策略评估
            boolean performEvaluation = Boolean.parseBoolean(props.getProperty("evaluation.enabled", "true"));
            if (performEvaluation) {
                Map<String, Object> evaluationParams = getEvaluationParameters(props);
                Map<String, Double> evaluationResults = StrategyEvaluator.evaluateStrategy(rawData, signals, evaluationParams);
                StrategyEvaluator.printEvaluationResults(evaluationResults);
            }

            // 9. 可视化
            boolean showChart = Boolean.parseBoolean(props.getProperty("visualization.show_chart", "true"));
            if (showChart) {
                visualizeResults(props, rawData, signals);
            }

        } catch (Exception e) {
            logger.error("Error in trading system: {}", e.getMessage(), e);
        }
    }

    /**
     * 加载配置文件
     */
    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        }
        return props;
    }

    /**
     * 初始化数据源
     */
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

    /**
     * 初始化预处理器
     */
    private static DataPreprocessor initPreprocessor(Properties props) {
        String type = props.getProperty("preprocessor.type", "standardization");
        DataPreprocessor preprocessor = PreprocessorFactory.createPreprocessor(type);

        Map<String, Double> params = new HashMap<>();
        // 添加预处理器参数（如果有）

        preprocessor.setParameters(params);
        return preprocessor;
    }

    /**
     * 执行传统交易策略
     */
    private static Map<LocalDate, TradeSignal> executeTraditionalStrategy(Properties props, List<StockData> rawData, List<ProcessedData> processedData) {
        String type = props.getProperty("strategy.type", "moving_average");
        TradingStrategy strategy = initStrategy(props);

        Map<LocalDate, TradeSignal> signals = new HashMap<>();

        // 计算移动平均线（用于可视化）
        int shortWindow = Integer.parseInt(props.getProperty("ma.short_window", "5"));
        int longWindow = Integer.parseInt(props.getProperty("ma.long_window", "20"));

        // 执行交易策略
        for (int i = 0; i < processedData.size(); i++) {
            ProcessedData current = processedData.get(i);
            // 使用当前数据点之前的历史数据生成信号
            List<ProcessedData> history = processedData.subList(
                    Math.min(i + 1, processedData.size() - 1),
                    processedData.size());

            TradeSignal signal = strategy.generateSignal(current, history);

            if (signal != TradeSignal.HOLD) {
                signals.put(current.getDate(), signal);
            }

            logger.info("{}: {} - Signal: {}", current.getDate(), rawData.get(i).getClose(), signal);
        }

        return signals;
    }

    /**
     * 执行机器学习策略
     */
    private static Map<LocalDate, TradeSignal> executeMLStrategy(Properties props, List<StockData> rawData, List<ProcessedData> processedData) {
        // 创建机器学习算法
        String algorithmType = props.getProperty("ml.algorithm", "randomforest");
        MachineLearningAlgorithm algorithm = createMLAlgorithm(props, algorithmType);

        // 创建机器学习策略
        MachineLearningStrategy mlStrategy = new MachineLearningStrategy(algorithm);

        // 设置策略参数
        Map<String, Double> strategyParams = new HashMap<>();
        strategyParams.put("lookbackWindow", Double.parseDouble(props.getProperty("ml.lookback_window", "10")));
        strategyParams.put("buyThreshold", Double.parseDouble(props.getProperty("ml.buy_threshold", "0.01")));
        strategyParams.put("sellThreshold", Double.parseDouble(props.getProperty("ml.sell_threshold", "-0.01")));
        mlStrategy.setParameters(strategyParams);

        // 设置特征名称（可选）
        String featureNamesStr = props.getProperty("ml.feature_names", "");
        if (!featureNamesStr.isEmpty()) {
            String[] featureNames = featureNamesStr.split(",");
            mlStrategy.setFeatureNames(featureNames);
        }

        // 训练模型
        logger.info("Training machine learning model...");
        mlStrategy.trainModel(rawData);

        // 生成交易信号
        Map<LocalDate, TradeSignal> signals = new HashMap<>();

        for (int i = 0; i < processedData.size(); i++) {
            ProcessedData current = processedData.get(i);
            List<ProcessedData> history = processedData.subList(
                    Math.min(i + 1, processedData.size() - 1),
                    processedData.size());

            TradeSignal signal = mlStrategy.generateSignal(current, history);

            if (signal != TradeSignal.HOLD) {
                signals.put(current.getDate(), signal);
            }

            logger.info("{}: {} - Signal: {}", current.getDate(), rawData.get(i).getClose(), signal);
        }

        // 可视化预测结果和特征重要性
        boolean showVisualization = Boolean.parseBoolean(props.getProperty("ml.show_visualization", "true"));
        if (showVisualization) {
            logger.info("Generating machine learning visualizations...");
            mlStrategy.visualizePredictions(rawData);
            mlStrategy.visualizeFeatureImportance();
        }

        return signals;
    }

    /**
     * 创建机器学习算法
     */
    private static MachineLearningAlgorithm createMLAlgorithm(Properties props, String algorithmType) {
        Map<String, Object> algorithmParams = new HashMap<>();

        if ("randomforest".equals(algorithmType) || "random_forest".equals(algorithmType)) {
            // 随机森林参数
            algorithmParams.put("numTrees", Integer.parseInt(props.getProperty("ml.rf.num_trees", "100")));
            algorithmParams.put("maxDepth", Integer.parseInt(props.getProperty("ml.rf.max_depth", "10")));
        }

        return MLAlgorithmFactory.createAlgorithm(algorithmType, algorithmParams);
    }

    /**
     * 初始化交易策略
     */
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

    /**
     * 获取评估参数
     */
    private static Map<String, Object> getEvaluationParameters(Properties props) {
        Map<String, Object> params = new HashMap<>();
        params.put("initialCapital", Double.parseDouble(props.getProperty("evaluation.initial_capital", "10000")));
        params.put("transactionFee", Double.parseDouble(props.getProperty("evaluation.transaction_fee", "0.001")));
        params.put("riskFreeRate", Double.parseDouble(props.getProperty("evaluation.risk_free_rate", "0.02")));
        params.put("tradingDaysPerYear", Integer.parseInt(props.getProperty("evaluation.trading_days_per_year", "252")));
        return params;
    }

    /**
     * 计算移动平均线
     */
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

    /**
     * 可视化结果
     */
    private static void visualizeResults(Properties props, List<StockData> rawData, Map<LocalDate, TradeSignal> signals) {
        String symbol = props.getProperty("trading.symbol", "AAPL");
        LocalDate startDate = LocalDate.parse(
                props.getProperty("trading.start_date", "2023-01-01"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endDate = LocalDate.parse(
                props.getProperty("trading.end_date", "2023-12-31"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 计算移动平均线
        int shortWindow = Integer.parseInt(props.getProperty("ma.short_window", "5"));
        int longWindow = Integer.parseInt(props.getProperty("ma.long_window", "20"));

        List<Double> shortMAList = calculateMovingAverages(rawData, shortWindow);
        List<Double> longMAList = calculateMovingAverages(rawData, longWindow);

        // 创建指标映射
        Map<String, List<Double>> indicators = new HashMap<>();
        indicators.put("Short MA (" + shortWindow + ")", shortMAList);
        indicators.put("Long MA (" + longWindow + ")", longMAList);

        // 转换信号格式
        Map<Date, TradeSignal> dateSignals = new HashMap<>();
        for (Map.Entry<LocalDate, TradeSignal> entry : signals.entrySet()) {
            Date date = Date.from(entry.getKey().atStartOfDay(ZoneId.systemDefault()).toInstant());
            dateSignals.put(date, entry.getValue());
        }

        // 创建图表
        String chartType = props.getProperty("visualization.chart_type", "line");
        String chartTitle = symbol + " Price Chart (" + startDate + " to " + endDate + ")";

        if ("candlestick".equalsIgnoreCase(chartType)) {
            ChartGenerator.createCandlestickChart(chartTitle, rawData, indicators, dateSignals);
        } else {
            ChartGenerator.createPriceMAChart(chartTitle, rawData, indicators, dateSignals);
        }


        // 保存图表（可选）
        boolean saveChart = Boolean.parseBoolean(props.getProperty("visualization.save_chart", "false"));
        if (saveChart) {
            String filePath = props.getProperty("visualization.save_path", "chart.png");
            // 图表保存逻辑将在ChartGenerator中实现
        }

    }
}