package com.quanttrading.evaluation.factory;

import com.quanttrading.evaluation.EvaluationMetric;
import com.quanttrading.evaluation.impl.*;

/**
 * 评价指标工厂类
 */
public class EvaluationMetricFactory {

    /**
     * 创建评价指标实例
     * @param type 指标类型
     * @return 评价指标实例
     */
    public static EvaluationMetric createMetric(String type) {
        switch (type.toLowerCase()) {
            case "mse":
            case "mean_squared_error":
                return new MeanSquaredError();
            case "annual_return":
            case "annualized_return":
                return new AnnualizedReturn();
            case "sharpe":
            case "sharpe_ratio":
                return new SharpeRatio();
            case "max_drawdown":
            case "maximum_drawdown":
                return new MaximumDrawdown();
            case "win_rate":
                return new WinRate();
            default:
                throw new IllegalArgumentException("Unsupported evaluation metric type: " + type);
        }
    }
}