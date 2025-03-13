// src/main/java/com/quanttrading/factory/DataSourceFactory.java
package com.quanttrading.factory;

import com.quanttrading.datasource.DataSource;
import com.quanttrading.datasource.LocalFileSource;
import com.quanttrading.datasource.YahooFinanceSource;

public class DataSourceFactory {
    public static DataSource createDataSource(String type) {
        switch (type.toLowerCase()) {
            case "yahoo":
                return new YahooFinanceSource();
            case "local":
                return new LocalFileSource();
            default:
                throw new IllegalArgumentException("Unsupported data source type: " + type);
        }
    }
}