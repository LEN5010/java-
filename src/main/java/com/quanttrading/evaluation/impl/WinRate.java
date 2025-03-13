package com.quanttrading.evaluation.impl;

import com.quanttrading.evaluation.EvaluationMetric;
import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;

import java.time.LocalDate;
import java.util.*;

/**
 * 胜率 (Win Rate)
 */
public class WinRate implements EvaluationMetric {

    @Override
    public double calculate(double[] predictions, double[] actuals) {
        // 对于胜率，这个方法不适用
        throw new UnsupportedOperationException("Win Rate is not applicable for prediction evaluation");
    }

    @Override
    public double calculate(List<StockData> stockData, Map<LocalDate, TradeSignal> signals, Map<String, Object> parameters) {
        if (stockData == null || stockData.isEmpty() || signals == null || signals.isEmpty()) {
            return 0.0;
        }

        // 获取参数
        double transactionFee = parameters.containsKey("transactionFee") ?
                (double) parameters.get("transactionFee") : 0.001; // 0.1%

        // 按日期排序股票数据
        stockData.sort(Comparator.comparing(StockData::getDate));

        // 识别交易
        List<Trade> trades = new ArrayList<>();
        TradeSignal lastSignal = null;
        double entryPrice = 0.0;
        LocalDate entryDate = null;

        for (StockData data : stockData) {
            LocalDate date = data.getDate();

            if (signals.containsKey(date)) {
                TradeSignal signal = signals.get(date);

                if (signal == TradeSignal.BUY && (lastSignal == null || lastSignal == TradeSignal.SELL)) {
                    // 开始买入交易
                    entryPrice = data.getClose();
                    entryDate = date;
                    lastSignal = TradeSignal.BUY;
                } else if (signal == TradeSignal.SELL && lastSignal == TradeSignal.BUY) {
                    // 结束买入交易
                    double exitPrice = data.getClose();
                    double profit = (exitPrice - entryPrice) / entryPrice - 2 * transactionFee; // 考虑双向交易费用
                    trades.add(new Trade(entryDate, date, entryPrice, exitPrice, profit));
                    lastSignal = TradeSignal.SELL;
                }
            }
        }

        // 如果最后一个交易还未平仓，使用最后一天的价格
        if (lastSignal == TradeSignal.BUY) {
            StockData lastData = stockData.get(stockData.size() - 1);
            double exitPrice = lastData.getClose();
            double profit = (exitPrice - entryPrice) / entryPrice - 2 * transactionFee;
            trades.add(new Trade(entryDate, lastData.getDate(), entryPrice, exitPrice, profit));
        }

        // 计算胜率
        if (trades.isEmpty()) {
            return 0.0;
        }

        long winningTrades = trades.stream().filter(t -> t.profit > 0).count();
        return (double) winningTrades / trades.size();
    }

    @Override
    public String getName() {
        return "Win Rate";
    }

    @Override
    public String getDescription() {
        return "The percentage of trades that result in a profit";
    }

    /**
     * 交易记录内部类
     */
    private static class Trade {
        LocalDate entryDate;
        LocalDate exitDate;
        double entryPrice;
        double exitPrice;
        double profit;

        Trade(LocalDate entryDate, LocalDate exitDate, double entryPrice, double exitPrice, double profit) {
            this.entryDate = entryDate;
            this.exitDate = exitDate;
            this.entryPrice = entryPrice;
            this.exitPrice = exitPrice;
            this.profit = profit;
        }
    }
}