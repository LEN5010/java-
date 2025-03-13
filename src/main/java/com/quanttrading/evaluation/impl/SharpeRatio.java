package com.quanttrading.evaluation.impl;

import com.quanttrading.evaluation.EvaluationMetric;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;

import java.time.LocalDate;
import java.util.*;

/**
 * 夏普比率 (Sharpe Ratio)
 */
public class SharpeRatio implements EvaluationMetric {

    @Override
    public double calculate(double[] predictions, double[] actuals) {
        // 对于夏普比率，这个方法不适用
        throw new UnsupportedOperationException("Sharpe Ratio is not applicable for prediction evaluation");
    }

    @Override
    public double calculate(List<StockData> stockData, Map<LocalDate, TradeSignal> signals, Map<String, Object> parameters) {
        if (stockData == null || stockData.isEmpty() || signals == null || signals.isEmpty()) {
            return 0.0;
        }

        // 获取参数
        double initialCapital = parameters.containsKey("initialCapital") ?
                (double) parameters.get("initialCapital") : 10000.0;
        double transactionFee = parameters.containsKey("transactionFee") ?
                (double) parameters.get("transactionFee") : 0.001; // 0.1%
        double riskFreeRate = parameters.containsKey("riskFreeRate") ?
                (double) parameters.get("riskFreeRate") : 0.02; // 2%
        int tradingDaysPerYear = parameters.containsKey("tradingDaysPerYear") ?
                (int) parameters.get("tradingDaysPerYear") : 252;

        // 按日期排序股票数据
        stockData.sort(Comparator.comparing(StockData::getDate));

        // 计算每日回报率
        List<Double> dailyReturns = new ArrayList<>();
        double capital = initialCapital;
        double shares = 0;
        double previousValue = initialCapital;

        for (StockData data : stockData) {
            LocalDate date = data.getDate();
            double price = data.getClose();

            // 计算当前投资组合价值
            double currentValue = capital + shares * price;

            // 计算日回报率
            if (previousValue > 0) {
                double dailyReturn = (currentValue - previousValue) / previousValue;
                dailyReturns.add(dailyReturn);
            }

            // 更新前一天的价值
            previousValue = currentValue;

            // 执行交易
            if (signals.containsKey(date)) {
                TradeSignal signal = signals.get(date);

                if (signal == TradeSignal.BUY && capital > 0) {
                    // 买入股票
                    double amount = capital;
                    double fee = amount * transactionFee;
                    shares = (amount - fee) / price;
                    capital = 0;
                } else if (signal == TradeSignal.SELL && shares > 0) {
                    // 卖出股票
                    double amount = shares * price;
                    double fee = amount * transactionFee;
                    capital = amount - fee;
                    shares = 0;
                }
            }
        }

        // 计算平均日回报率
        double averageDailyReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // 计算日回报率标准差
        double sumSquaredDiff = dailyReturns.stream()
                .mapToDouble(r -> Math.pow(r - averageDailyReturn, 2))
                .sum();
        double dailyStdDev = Math.sqrt(sumSquaredDiff / (dailyReturns.size() - 1));

        // 计算年化回报率
        double annualizedReturn = averageDailyReturn * tradingDaysPerYear;

        // 计算年化风险（波动率）
        double annualizedRisk = dailyStdDev * Math.sqrt(tradingDaysPerYear);

        // 计算年化风险自由率
        double dailyRiskFreeRate = Math.pow(1 + riskFreeRate, 1.0 / tradingDaysPerYear) - 1;

        // 计算夏普比率
        double sharpeRatio = (annualizedReturn - riskFreeRate) / annualizedRisk;

        return sharpeRatio;
    }

    @Override
    public String getName() {
        return "Sharpe Ratio";
    }

    @Override
    public String getDescription() {
        return "A measure of risk-adjusted return, calculated as the ratio of excess return to volatility";
    }
}