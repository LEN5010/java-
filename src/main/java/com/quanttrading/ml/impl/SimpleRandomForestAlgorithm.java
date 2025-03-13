package com.quanttrading.ml.impl;

import com.quanttrading.ml.MachineLearningAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * 简化版随机森林实现
 */
public class SimpleRandomForestAlgorithm implements MachineLearningAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(SimpleRandomForestAlgorithm.class);

    private List<DecisionTree> trees;
    private int numTrees;
    private int maxDepth;
    private Random random;
    private String[] featureNames;
    private Map<String, Double> featureImportance;

    public SimpleRandomForestAlgorithm() {
        this(100, 10);
        this.featureImportance = new HashMap<>();
    }

    public SimpleRandomForestAlgorithm(int numTrees, int maxDepth) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.random = new Random(42);
        this.trees = new ArrayList<>();
        this.featureImportance = new HashMap<>();
    }

    public void setFeatureNames(String[] featureNames) {
        this.featureNames = featureNames;
    }

    @Override
    public void train(double[][] features, double[] labels) {
        if (features.length != labels.length) {
            throw new IllegalArgumentException("Features and labels must have the same length");
        }

        trees.clear();

        // 创建多棵决策树
        for (int i = 0; i < numTrees; i++) {
            // 创建bootstrap样本
            int[] indices = bootstrap(features.length);
            double[][] bootstrapFeatures = new double[indices.length][];
            double[] bootstrapLabels = new double[indices.length];

            for (int j = 0; j < indices.length; j++) {
                bootstrapFeatures[j] = features[indices[j]];
                bootstrapLabels[j] = labels[indices[j]];
            }

            // 创建并训练决策树
            DecisionTree tree = new DecisionTree(maxDepth, random);
            tree.train(bootstrapFeatures, bootstrapLabels);
            trees.add(tree);

            logger.debug("Tree {}/{} trained", i+1, numTrees);
        }

        // 计算特征重要性
        calculateFeatureImportance(features[0].length);

        logger.info("Random Forest model trained with {} trees", numTrees);
    }

    private void calculateFeatureImportance(int numFeatures) {
        featureImportance.clear();

        // 初始化特征重要性计数器
        int[] featureUsageCount = new int[numFeatures];

        // 统计每个特征在所有树中被用作分割节点的次数
        for (DecisionTree tree : trees) {
            tree.countFeatureUsage(featureUsageCount);
        }

        // 计算每个特征的相对重要性
        int totalUsage = Arrays.stream(featureUsageCount).sum();
        if (totalUsage > 0) {
            for (int i = 0; i < numFeatures; i++) {
                String featureName = (featureNames != null && i < featureNames.length) ?
                        featureNames[i] : "Feature " + i;
                double importance = (double) featureUsageCount[i] / totalUsage;
                featureImportance.put(featureName, importance);
            }
        }
    }

    private int[] bootstrap(int size) {
        int[] indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = random.nextInt(size);
        }
        return indices;
    }

    @Override
    public double predict(double[] features) {
        if (trees.isEmpty()) {
            throw new IllegalStateException("Model not trained yet");
        }

        // 计算所有树的平均预测值
        double sum = 0.0;
        for (DecisionTree tree : trees) {
            sum += tree.predict(features);
        }

        return sum / trees.size();
    }

    @Override
    public double[] predict(double[][] features) {
        if (trees.isEmpty()) {
            throw new IllegalStateException("Model not trained yet");
        }

        int numSamples = features.length;
        double[] predictions = new double[numSamples];

        for (int i = 0; i < numSamples; i++) {
            predictions[i] = predict(features[i]);
        }

        return predictions;
    }

    @Override
    public void saveModel(String path) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(trees);
            logger.info("Random Forest model saved to: {}", path);
        } catch (IOException e) {
            logger.error("Error saving model: {}", e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadModel(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            trees = (List<DecisionTree>) ois.readObject();
            logger.info("Random Forest model loaded from: {}", path);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Error loading model: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("numTrees", numTrees);
        params.put("maxDepth", maxDepth);
        return params;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("numTrees")) {
            this.numTrees = (int) parameters.get("numTrees");
        }
        if (parameters.containsKey("maxDepth")) {
            this.maxDepth = (int) parameters.get("maxDepth");
        }
    }

    @Override
    public Map<String, Double> getFeatureImportance() {
        return new HashMap<>(featureImportance);
    }

    /**
     * 简单决策树实现
     */
    private static class DecisionTree implements Serializable {
        private static final long serialVersionUID = 1L;

        private Node root;
        private int maxDepth;
        private Random random;

        public DecisionTree(int maxDepth, Random random) {
            this.maxDepth = maxDepth;
            this.random = random;
        }

        public void train(double[][] features, double[] labels) {
            root = buildTree(features, labels, 0);
        }

        private Node buildTree(double[][] features, double[] labels, int depth) {
            if (depth >= maxDepth || features.length <= 5) {
                // 创建叶子节点
                return new LeafNode(calculateAverage(labels));
            }

            // 随机选择特征
            int featureIndex = random.nextInt(features[0].length);

            // 找出最佳分割点
            double splitValue = findBestSplit(features, labels, featureIndex);

            // 分割数据
            List<Integer> leftIndices = new ArrayList<>();
            List<Integer> rightIndices = new ArrayList<>();

            for (int i = 0; i < features.length; i++) {
                if (features[i][featureIndex] <= splitValue) {
                    leftIndices.add(i);
                } else {
                    rightIndices.add(i);
                }
            }

            // 如果分割不成功，创建叶子节点
            if (leftIndices.isEmpty() || rightIndices.isEmpty()) {
                return new LeafNode(calculateAverage(labels));
            }

            // 创建左右子树的数据
            double[][] leftFeatures = new double[leftIndices.size()][];
            double[] leftLabels = new double[leftIndices.size()];
            double[][] rightFeatures = new double[rightIndices.size()][];
            double[] rightLabels = new double[rightIndices.size()];

            for (int i = 0; i < leftIndices.size(); i++) {
                int index = leftIndices.get(i);
                leftFeatures[i] = features[index];
                leftLabels[i] = labels[index];
            }

            for (int i = 0; i < rightIndices.size(); i++) {
                int index = rightIndices.get(i);
                rightFeatures[i] = features[index];
                rightLabels[i] = labels[index];
            }

            // 递归构建左右子树
            Node leftChild = buildTree(leftFeatures, leftLabels, depth + 1);
            Node rightChild = buildTree(rightFeatures, rightLabels, depth + 1);

            return new SplitNode(featureIndex, splitValue, leftChild, rightChild);
        }

        private double findBestSplit(double[][] features, double[] labels, int featureIndex) {
            // 简化版：使用特征的平均值作为分割点
            double sum = 0.0;
            for (double[] feature : features) {
                sum += feature[featureIndex];
            }
            return sum / features.length;
        }

        private double calculateAverage(double[] values) {
            double sum = 0.0;
            for (double value : values) {
                sum += value;
            }
            return values.length > 0 ? sum / values.length : 0.0;
        }

        public double predict(double[] features) {
            return root.predict(features);
        }

        public void countFeatureUsage(int[] featureUsageCount) {
            if (root != null) {
                root.countFeatureUsage(featureUsageCount);
            }
        }

        /**
         * 决策树节点接口
         */
        private interface Node extends Serializable {
            double predict(double[] features);
            void countFeatureUsage(int[] featureUsageCount);
        }

        /**
         * 分割节点
         */
        private static class SplitNode implements Node {
            private static final long serialVersionUID = 1L;

            private int featureIndex;
            private double splitValue;
            private Node leftChild;
            private Node rightChild;

            public SplitNode(int featureIndex, double splitValue, Node leftChild, Node rightChild) {
                this.featureIndex = featureIndex;
                this.splitValue = splitValue;
                this.leftChild = leftChild;
                this.rightChild = rightChild;
            }

            @Override
            public double predict(double[] features) {
                if (features[featureIndex] <= splitValue) {
                    return leftChild.predict(features);
                } else {
                    return rightChild.predict(features);
                }
            }

            @Override
            public void countFeatureUsage(int[] featureUsageCount) {
                // 增加当前特征的使用计数
                if (featureIndex < featureUsageCount.length) {
                    featureUsageCount[featureIndex]++;
                }

                // 递归统计子节点
                leftChild.countFeatureUsage(featureUsageCount);
                rightChild.countFeatureUsage(featureUsageCount);
            }
        }

        /**
         * 叶子节点
         */
        private static class LeafNode implements Node {
            private static final long serialVersionUID = 1L;

            private double prediction;

            public LeafNode(double prediction) {
                this.prediction = prediction;
            }

            @Override
            public double predict(double[] features) {
                return prediction;
            }

            @Override
            public void countFeatureUsage(int[] featureUsageCount) {
                // 叶子节点不使用特征进行分割，无需操作
            }
        }
    }
}