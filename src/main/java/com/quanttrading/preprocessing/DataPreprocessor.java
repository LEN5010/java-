// src/main/java/com/quanttrading/preprocessing/DataPreprocessor.java
package com.quanttrading.preprocessing;

import com.quanttrading.model.ProcessedData;
import com.quanttrading.model.StockData;
import java.util.List;
import java.util.Map;

public interface DataPreprocessor {
    List<ProcessedData> process(List<StockData> rawData);
    void setParameters(Map<String, Double> params);
}