package com.quanttrading.ml;

import java.util.List;
import java.util.Map;

/**
 * 机器学习算法接口
 */
public interface MachineLearningAlgorithm {
    /**
     * 训练模型
     * @param features 特征数据
     * @param labels 标签数据
     */
    void train(double[][] features, double[] labels);

    /**
     * 预测单个样本
     * @param features 单个样本的特征
     * @return 预测结果
     */
    double predict(double[] features);

    /**
     * 批量预测
     * @param features 多个样本的特征
     * @return 预测结果数组
     */
    double[] predict(double[][] features);

    /**
     * 保存模型
     * @param path 保存路径
     */
    void saveModel(String path);

    /**
     * 加载模型
     * @param path 模型路径
     */
    void loadModel(String path);

    /**
     * 获取模型参数
     * @return 模型参数映射
     */
    Map<String, Object> getParameters();

    /**
     * 设置模型参数
     * @param parameters 参数映射
     */
    void setParameters(Map<String, Object> parameters);

    /**
     * 获取特征重要性（如果算法支持）
     * @return 特征名称到重要性的映射
     */
    default Map<String, Double> getFeatureImportance() {
        return Map.of();
    }
}