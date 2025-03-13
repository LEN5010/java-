package com.quanttrading.ml.impl;

import com.quanttrading.ml.MachineLearningAlgorithm;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于DL4J的LSTM实现
 */
public class LSTMAlgorithm implements MachineLearningAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(LSTMAlgorithm.class);

    private MultiLayerNetwork model;
    private int inputSize;
    private int hiddenSize;
    private int outputSize;
    private double learningRate;
    private int numEpochs;
    private int timeSeriesLength;

    public LSTMAlgorithm() {
        this(10, 50, 1, 0.01, 100, 10);
    }

    public LSTMAlgorithm(int inputSize, int hiddenSize, int outputSize,
                         double learningRate, int numEpochs, int timeSeriesLength) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.numEpochs = numEpochs;
        this.timeSeriesLength = timeSeriesLength;

        initModel();
    }

    private void initModel() {
        // 配置LSTM网络
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(learningRate))
                .l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(inputSize)
                        .nOut(hiddenSize)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(hiddenSize)
                        .nOut(outputSize)
                        .build())
                .backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(timeSeriesLength)
                .tBPTTBackwardLength(timeSeriesLength)
                .build();

        model = new MultiLayerNetwork(conf);
        model.init();

        logger.info("LSTM model initialized with inputSize={}, hiddenSize={}, outputSize={}",
                inputSize, hiddenSize, outputSize);
    }

    @Override
    public void train(double[][] features, double[] labels) {
        if (features.length != labels.length) {
            throw new IllegalArgumentException("Features and labels must have the same length");
        }

        // 将输入数据转换为DL4J的格式 [samples, features, timeSteps]
        int numSamples = features.length - timeSeriesLength;

        for (int epoch = 0; epoch < numEpochs; epoch++) {
            double totalLoss = 0;

            for (int i = 0; i < numSamples; i++) {
                // 创建时间序列数据
                INDArray input = Nd4j.zeros(1, inputSize, timeSeriesLength);
                for (int t = 0; t < timeSeriesLength; t++) {
                    double[] featureVector = features[i + t];
                    for (int j = 0; j < inputSize && j < featureVector.length; j++) {
                        input.putScalar(new int[]{0, j, t}, featureVector[j]);
                    }
                }

                // 创建标签
                INDArray output = Nd4j.zeros(1, outputSize, timeSeriesLength);
                output.putScalar(new int[]{0, 0, timeSeriesLength - 1}, labels[i + timeSeriesLength]);

                // 创建数据集
                DataSet dataSet = new DataSet(input, output);

                // 训练模型
                model.fit(dataSet);

                // 计算损失
                totalLoss += model.score(dataSet);
            }

            double avgLoss = totalLoss / numSamples;
            logger.info("Epoch {}/{}: Average Loss = {}", epoch + 1, numEpochs, avgLoss);
        }

        logger.info("Training completed");
    }

    @Override
    public double predict(double[] features) {
        if (features.length < timeSeriesLength * inputSize) {
            throw new IllegalArgumentException("Insufficient features for prediction");
        }

        // 创建时间序列数据
        INDArray input = Nd4j.zeros(1, inputSize, timeSeriesLength);
        for (int t = 0; t < timeSeriesLength; t++) {
            for (int j = 0; j < inputSize; j++) {
                input.putScalar(new int[]{0, j, t}, features[t * inputSize + j]);
            }
        }

        // 使用模型预测
        INDArray output = model.output(input);

        // 返回最后一个时间步的预测值
        return output.getDouble(0, 0, timeSeriesLength - 1);
    }

    @Override
    public double[] predict(double[][] features) {
        int numSamples = features.length - timeSeriesLength + 1;
        double[] predictions = new double[numSamples];

        for (int i = 0; i < numSamples; i++) {
            // 创建时间序列数据
            INDArray input = Nd4j.zeros(1, inputSize, timeSeriesLength);
            for (int t = 0; t < timeSeriesLength; t++) {
                double[] featureVector = features[i + t];
                for (int j = 0; j < inputSize && j < featureVector.length; j++) {
                    input.putScalar(new int[]{0, j, t}, featureVector[j]);
                }
            }

            // 使用模型预测
            INDArray output = model.output(input);

            // 获取最后一个时间步的预测值
            predictions[i] = output.getDouble(0, 0, timeSeriesLength - 1);
        }

        return predictions;
    }

    @Override
    public void saveModel(String path) {
        try {
            File locationToSave = new File(path);
            ModelSerializer.writeModel(model, locationToSave, true);
            logger.info("Model saved to: {}", path);
        } catch (IOException e) {
            logger.error("Error saving model: {}", e.getMessage());
        }
    }

    @Override
    public void loadModel(String path) {
        try {
            File locationToLoad = new File(path);
            model = ModelSerializer.restoreMultiLayerNetwork(locationToLoad);
            logger.info("Model loaded from: {}", path);
        } catch (IOException e) {
            logger.error("Error loading model: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("inputSize", inputSize);
        params.put("hiddenSize", hiddenSize);
        params.put("outputSize", outputSize);
        params.put("learningRate", learningRate);
        params.put("numEpochs", numEpochs);
        params.put("timeSeriesLength", timeSeriesLength);
        return params;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("inputSize")) {
            this.inputSize = (int) parameters.get("inputSize");
        }
        if (parameters.containsKey("hiddenSize")) {
            this.hiddenSize = (int) parameters.get("hiddenSize");
        }
        if (parameters.containsKey("outputSize")) {
            this.outputSize = (int) parameters.get("outputSize");
        }
        if (parameters.containsKey("learningRate")) {
            this.learningRate = (double) parameters.get("learningRate");
        }
        if (parameters.containsKey("numEpochs")) {
            this.numEpochs = (int) parameters.get("numEpochs");
        }
        if (parameters.containsKey("timeSeriesLength")) {
            this.timeSeriesLength = (int) parameters.get("timeSeriesLength");
        }

        // 重新初始化模型
        initModel();
    }
}