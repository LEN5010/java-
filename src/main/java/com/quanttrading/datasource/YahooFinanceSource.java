// src/main/java/com/quanttrading/datasource/YahooFinanceSource.java
package com.quanttrading.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrading.model.StockData;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YahooFinanceSource implements DataSource {
    private static final Logger logger = LoggerFactory.getLogger(YahooFinanceSource.class);
    private static final String API_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private CloseableHttpClient httpClient;
    private int timeout = 5000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YahooFinanceSource() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public void configure(Map<String, Object> config) {
        if (config.containsKey("timeout")) {
            this.timeout = (Integer) config.get("timeout");
        }
    }

    @Override
    public List<StockData> fetchHistoricalData(String symbol, LocalDate start, LocalDate end) throws Exception {
        String url = buildUrl(symbol, start, end);
        HttpGet request = new HttpGet(url);

        logger.info("Fetching data from: {}", url);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            return parseResponse(symbol, responseBody);
        } catch (Exception e) {
            logger.error("Error fetching data: {}", e.getMessage());
            throw e;
        }
    }

    private String buildUrl(String symbol, LocalDate start, LocalDate end) {
        long startEpoch = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long endEpoch = end.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        return String.format("%s%s?period1=%d&period2=%d&interval=1d&events=history",
                API_URL, symbol, startEpoch, endEpoch);
    }

    private List<StockData> parseResponse(String symbol, String jsonResponse) throws Exception {
        List<StockData> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);

        JsonNode chart = root.path("chart");
        JsonNode result_node = chart.path("result").get(0);

        JsonNode timestamps = result_node.path("timestamp");
        JsonNode indicators = result_node.path("indicators");
        JsonNode quote = indicators.path("quote").get(0);

        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i).asLong();
            LocalDate date = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            double open = opens.get(i).asDouble();
            double high = highs.get(i).asDouble();
            double low = lows.get(i).asDouble();
            double close = closes.get(i).asDouble();
            long volume = volumes.get(i).asLong();

            result.add(new StockData(symbol, date, open, high, low, close, volume));
        }

        return result;
    }
}