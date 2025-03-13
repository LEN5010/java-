// src/main/java/com/quanttrading/datasource/DataSource.java
package com.quanttrading.datasource;

import com.quanttrading.model.StockData;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DataSource {
    List<StockData> fetchHistoricalData(String symbol, LocalDate start, LocalDate end) throws Exception;
    void configure(Map<String, Object> config);
}