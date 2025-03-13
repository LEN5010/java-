package com.quanttrading.evaluation;

import com.quanttrading.evaluation.factory.EvaluationMetricFactory;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略评估工具类
 */
public class StrategyEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(StrategyEvaluator.class);

    /**
     * 评估策略性能
     * @param stockData 股票数据
     * @param signals 交易信号
     * @return 评估结果
     */
    public static Map<String, Double> evaluateStrategy(List<StockData> stockData, Map<LocalDate, TradeSignal> signals) {
        return evaluateStrategy(stockData, signals, null);
    }

    /**
     * 评估策略性能
     * @param stockData 股票数据
     * @param signals 交易信号
     * @param parameters 评估参数
     * @return 评估结果
     */
    public static Map<String, Double> evaluateStrategy(List<StockData> stockData, Map<LocalDate, TradeSignal> signals, Map<String, Object> parameters) {
        if (parameters == null) {
            parameters = new HashMap<>();
            parameters.put("initialCapital", 10000.0);
            parameters.put("transactionFee", 0.001);
            parameters.put("riskFreeRate", 0.02);
            parameters.put("tradingDaysPerYear", 252);
        }

        Map<String, Double> results = new HashMap<>();

        // 计算各项指标
        try {
            // 年化收益率
            EvaluationMetric annualReturn = EvaluationMetricFactory.createMetric("annual_return");
            double annualReturnValue = annualReturn.calculate(stockData, signals, parameters);
            results.put("annualReturn", annualReturnValue);

            // 夏普比率
            EvaluationMetric sharpeRatio = EvaluationMetricFactory.createMetric("sharpe_ratio");
            double sharpeRatioValue = sharpeRatio.calculate(stockData, signals, parameters);
            results.put("sharpeRatio", sharpeRatioValue);

            // 最大回撤
            EvaluationMetric maxDrawdown = EvaluationMetricFactory.createMetric("max_drawdown");
            double maxDrawdownValue = maxDrawdown.calculate(stockData, signals, parameters);
            results.put("maxDrawdown", maxDrawdownValue);

            // 胜率
            EvaluationMetric winRate = EvaluationMetricFactory.createMetric("win_rate");
            double winRateValue = winRate.calculate(stockData, signals, parameters);
            results.put("winRate", winRateValue);

            logger.info("Strategy evaluation completed with {} metrics", results.size());
        } catch (Exception e) {
            logger.error("Error evaluating strategy: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 打印评估结果
     * @param results 评估结果
     */
    public static void printEvaluationResults(Map<String, Double> results) {
        System.out.println("\n====== Strategy Evaluation Results ======");
        System.out.printf("Annual Return: %.2f%%\n", results.getOrDefault("annualReturn", 0.0) * 100);
        System.out.printf("Sharpe Ratio: %.2f\n", results.getOrDefault("sharpeRatio", 0.0));
        System.out.printf("Maximum Drawdown: %.2f%%\n", results.getOrDefault("maxDrawdown", 0.0) * 100);
        System.out.printf("Win Rate: %.2f%%\n", results.getOrDefault("winRate", 0.0) * 100);
        System.out.println("=========================================\n");
    }
}