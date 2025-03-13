package com.quanttrading.evaluation.impl;

import com.quanttrading.evaluation.EvaluationMetric;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 年化收益率 (Annualized Return)
 */
public class AnnualizedReturn implements EvaluationMetric {

    @Override
    public double calculate(double[] predictions, double[] actuals) {
        // 对于年化收益率，这个方法不适用
        throw new UnsupportedOperationException("Annualized Return is not applicable for prediction evaluation");
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

        // 计算策略收益
        double capital = initialCapital;
        double shares = 0;
        LocalDate startDate = stockData.get(0).getDate();
        LocalDate endDate = stockData.get(stockData.size() - 1).getDate();

        for (StockData data : stockData) {
            LocalDate date = data.getDate();
            double price = data.getClose();

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

        // 计算最终价值
        double finalValue = capital;
        if (shares > 0) {
            // 如果还持有股票，按最后一天的价格卖出
            double lastPrice = stockData.get(stockData.size() - 1).getClose();
            double amount = shares * lastPrice;
            double fee = amount * transactionFee;
            finalValue = capital + amount - fee;
        }

        // 计算总收益率
        double totalReturn = (finalValue - initialCapital) / initialCapital;

        // 计算投资天数
        long days = ChronoUnit.DAYS.between(startDate, endDate);

        // 计算年化收益率
        double yearsInvested = days / 365.0;
        double annualizedReturn = Math.pow(1 + totalReturn, 1 / yearsInvested) - 1;

        return annualizedReturn;
    }

    @Override
    public String getName() {
        return "Annualized Return";
    }

    @Override
    public String getDescription() {
        return "The average annual rate of return of a trading strategy";
    }
}