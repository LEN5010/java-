package com.quanttrading.evaluation;

import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 评价指标接口
 */
public interface EvaluationMetric {
    /**
     * 计算评价指标
     * @param predictions 预测值
     * @param actuals 实际值
     * @return 指标值
     */
    double calculate(double[] predictions, double[] actuals);

    /**
     * 计算交易策略的评价指标
     * @param stockData 股票数据
     * @param signals 交易信号
     * @param parameters 计算参数
     * @return 指标值
     */
    double calculate(List<StockData> stockData, Map<LocalDate, TradeSignal> signals, Map<String, Object> parameters);

    /**
     * 获取指标名称
     * @return 指标名称
     */
    String getName();

    /**
     * 获取指标描述
     * @return 指标描述
     */
    String getDescription();
}