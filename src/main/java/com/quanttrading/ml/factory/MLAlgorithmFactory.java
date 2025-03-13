package com.quanttrading.ml.factory;

import com.quanttrading.ml.MachineLearningAlgorithm;
import com.quanttrading.ml.impl.SimpleRandomForestAlgorithm;
import com.quanttrading.ml.impl.WekaRandomForestAlgorithm;

import java.util.Map;

/**
 * 机器学习算法工厂类
 */
public class MLAlgorithmFactory {

    /**
     * 创建机器学习算法实例
     * @param type 算法类型
     * @return 算法实例
     */
    public static MachineLearningAlgorithm createAlgorithm(String type) {
        return createAlgorithm(type, null);
    }

    /**
     * 创建机器学习算法实例并设置参数
     * @param type 算法类型
     * @param parameters 算法参数
     * @return 算法实例
     */
    public static MachineLearningAlgorithm createAlgorithm(String type, Map<String, Object> parameters) {
        MachineLearningAlgorithm algorithm;

        switch (type.toLowerCase()) {
            case "weka_rf":
            case "weka_randomforest":
                algorithm = new WekaRandomForestAlgorithm();
                break;
            case "randomforest":
            case "random_forest":
            case "simple_randomforest":
                algorithm = new SimpleRandomForestAlgorithm();
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm type: " + type);
        }

        if (parameters != null) {
            algorithm.setParameters(parameters);
        }

        return algorithm;
    }
}