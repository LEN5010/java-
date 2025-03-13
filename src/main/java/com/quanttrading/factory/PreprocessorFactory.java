package com.quanttrading.factory;

import com.quanttrading.preprocessing.DataPreprocessor;
import com.quanttrading.preprocessing.StandardizationProcessor;

public class PreprocessorFactory {
    public static DataPreprocessor createPreprocessor(String type) {
        switch (type.toLowerCase()) {
            case "standardization":
                return new StandardizationProcessor();
            default:
                throw new IllegalArgumentException("Unsupported preprocessor type: " + type);
        }
    }
}