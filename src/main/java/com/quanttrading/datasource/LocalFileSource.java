// src/main/java/com/quanttrading/datasource/LocalFileSource.java
package com.quanttrading.datasource;

import com.quanttrading.model.StockData;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalFileSource implements DataSource {
    private String dataDirectory;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void configure(Map<String, Object> config) {
        if (config.containsKey("dataDirectory")) {
            this.dataDirectory = (String) config.get("dataDirectory");
        }
        if (config.containsKey("dateFormat")) {
            this.dateFormatter = DateTimeFormatter.ofPattern((String) config.get("dateFormat"));
        }
    }

    @Override
    public List<StockData> fetchHistoricalData(String symbol, LocalDate start, LocalDate end) throws Exception {
        List<StockData> result = new ArrayList<>();
        String filePath = dataDirectory + "/" + symbol + ".csv";

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip header
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                LocalDate date = LocalDate.parse(parts[0], dateFormatter);

                // Only include data within the date range
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    result.add(new StockData(
                            symbol,
                            date,
                            Double.parseDouble(parts[1]), // open
                            Double.parseDouble(parts[2]), // high
                            Double.parseDouble(parts[3]), // low
                            Double.parseDouble(parts[4]), // close
                            Long.parseLong(parts[5])      // volume
                    ));
                }
            }
        }

        return result;
    }
}