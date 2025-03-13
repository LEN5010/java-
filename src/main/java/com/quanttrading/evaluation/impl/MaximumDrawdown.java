package com.quanttrading.evaluation.impl;

import com.quanttrading.evaluation.EvaluationMetric;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;

import java.time.LocalDate;
import java.util.*;

/**
 * 最大回撤 (Maximum Drawdown)
 */
public class MaximumDrawdown implements EvaluationMetric {

    @Override
    public double calculate(double[] predictions, double[] actuals) {
        // 对于最大回撤，这个方法不适用
        throw new UnsupportedOperationException("Maximum Drawdown is not applicable for prediction evaluation");
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

        // 按日期排序股票数据
        stockData.sort(Comparator.comparing(StockData::getDate));

        // 计算每日投资组合价值
        List<Double> portfolioValues = new ArrayList<>();
        double capital = initialCapital;
        double shares = 0;

        for (StockData data : stockData) {
            LocalDate date = data.getDate();
            double price = data.getClose();

            // 计算当前投资组合价值
            double currentValue = capital + shares * price;
            portfolioValues.add(currentValue);

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

        // 计算最大回撤
        double maxDrawdown = 0.0;
        double peak = portfolioValues.get(0);

        for (double value : portfolioValues) {
            if (value > peak) {
                peak = value;
            }

            double drawdown = (peak - value) / peak;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }

    @Override
    public String getName() {
        return "Maximum Drawdown";
    }

    @Override
    public String getDescription() {
        return "The largest percentage drop in portfolio value from a peak to a trough";
    }
}