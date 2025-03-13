package com.quanttrading.visualization;

import com.quanttrading.model.StockData;
import com.quanttrading.model.TradeSignal;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChartGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ChartGenerator.class);

    /**
     * 创建并显示价格和移动平均线图表
     */
    public static void createPriceMAChart(String title, List<StockData> stockData,
                                          Map<String, List<Double>> indicators,
                                          Map<Date, TradeSignal> signals) {
        // 创建价格时间序列
        TimeSeries priceSeries = new TimeSeries("Price");

        // 添加价格数据
        for (StockData data : stockData) {
            Date date = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            priceSeries.add(new Day(date), data.getClose());
        }

        // 创建数据集合
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(priceSeries);

        // 添加指标数据（如移动平均线）
        for (Map.Entry<String, List<Double>> entry : indicators.entrySet()) {
            TimeSeries indicatorSeries = new TimeSeries(entry.getKey());
            List<Double> values = entry.getValue();

            for (int i = 0; i < values.size() && i < stockData.size(); i++) {
                Date date = Date.from(stockData.get(i).getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                indicatorSeries.add(new Day(date), values.get(i));
            }

            dataset.addSeries(indicatorSeries);
        }

        // 创建图表
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,      // 标题
                "Date",     // x轴标签
                "Price",    // y轴标签
                dataset,    // 数据
                true,       // 显示图例
                true,       // 显示工具提示
                false       // 不生成URL
        );

        // 自定义图表外观
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 自定义日期格式
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

        // 设置线条和形状渲染器
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // 价格线（第一条线）
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, false);

        // 设置指标线的颜色和粗细
        int index = 1;
        for (String key : indicators.keySet()) {
            Color color = (key.contains("Short")) ? Color.RED : Color.GREEN;
            renderer.setSeriesPaint(index, color);
            renderer.setSeriesStroke(index, new BasicStroke(1.5f));
            renderer.setSeriesShapesVisible(index, false);
            index++;
        }

        // 添加交易信号标记
        if (signals != null && !signals.isEmpty()) {
            TimeSeries buySeries = new TimeSeries("Buy");
            TimeSeries sellSeries = new TimeSeries("Sell");

            for (Map.Entry<Date, TradeSignal> entry : signals.entrySet()) {
                Date date = entry.getKey();
                Day day = new Day(date);

                // 获取该日期的价格
                double price = 0;
                for (StockData data : stockData) {
                    Date dataDate = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    if (day.equals(new Day(dataDate))) {
                        price = data.getClose();
                        break;
                    }
                }

                if (entry.getValue() == TradeSignal.BUY) {
                    buySeries.add(day, price);
                } else if (entry.getValue() == TradeSignal.SELL) {
                    sellSeries.add(day, price);
                }
            }

            // 添加买入/卖出信号到数据集
            TimeSeriesCollection signalDataset = new TimeSeriesCollection();
            signalDataset.addSeries(buySeries);
            signalDataset.addSeries(sellSeries);

            // 创建信号渲染器
            XYLineAndShapeRenderer signalRenderer = new XYLineAndShapeRenderer();
            signalRenderer.setSeriesPaint(0, Color.GREEN);  // 买入信号颜色
            signalRenderer.setSeriesPaint(1, Color.RED);    // 卖出信号颜色
            signalRenderer.setSeriesShapesVisible(0, true);
            signalRenderer.setSeriesShapesVisible(1, true);
            signalRenderer.setSeriesLinesVisible(0, false);
            signalRenderer.setSeriesLinesVisible(1, false);
            signalRenderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
            signalRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));

            // 添加第二个渲染器和数据集
            plot.setDataset(1, signalDataset);
            plot.setRenderer(1, signalRenderer);
        }

        plot.setRenderer(0, renderer);

        // 显示图表
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1000, 600));

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        logger.info("Chart created and displayed: {}", title);
    }

    /**
     * 创建蜡烛图
     */
    public static void createCandlestickChart(String title, List<StockData> stockData,
                                              Map<String, List<Double>> indicators,
                                              Map<Date, TradeSignal> signals) {
        // 创建蜡烛图数据集
        OHLCDataset candlestickDataset = createCandlestickDataset(stockData);

        // 创建蜡烛图
        JFreeChart chart = ChartFactory.createCandlestickChart(
                title,          // 标题
                "Date",         // x轴标签
                "Price",        // y轴标签
                candlestickDataset,  // 数据
                true            // 显示图例
        );

        // 自定义图表外观
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 自定义日期格式
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

        // 自定义蜡烛图渲染器
        CandlestickRenderer renderer = (CandlestickRenderer) plot.getRenderer();
        renderer.setUpPaint(Color.GREEN);
        renderer.setDownPaint(Color.RED);

        // 添加移动平均线
        if (indicators != null && !indicators.isEmpty()) {
            TimeSeriesCollection maDataset = new TimeSeriesCollection();

            for (Map.Entry<String, List<Double>> entry : indicators.entrySet()) {
                TimeSeries series = new TimeSeries(entry.getKey());
                List<Double> values = entry.getValue();

                for (int i = 0; i < values.size() && i < stockData.size(); i++) {
                    Date date = Date.from(stockData.get(i).getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    series.add(new Day(date), values.get(i));
                }

                maDataset.addSeries(series);
            }

            // 添加移动平均线到图表
            plot.setDataset(1, maDataset);

            // 创建线条渲染器
            XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer();

            int index = 0;
            for (String key : indicators.keySet()) {
                Color color = (key.contains("Short")) ? Color.RED : Color.GREEN;
                maRenderer.setSeriesPaint(index, color);
                maRenderer.setSeriesStroke(index, new BasicStroke(1.5f));
                maRenderer.setSeriesShapesVisible(index, false);
                index++;
            }

            plot.setRenderer(1, maRenderer);
        }

        // 添加交易信号
        if (signals != null && !signals.isEmpty()) {
            TimeSeriesCollection signalDataset = new TimeSeriesCollection();
            TimeSeries buySeries = new TimeSeries("Buy");
            TimeSeries sellSeries = new TimeSeries("Sell");

            for (Map.Entry<Date, TradeSignal> entry : signals.entrySet()) {
                Date date = entry.getKey();
                Day day = new Day(date);

                // 获取该日期的价格
                double price = 0;
                for (StockData data : stockData) {
                    Date dataDate = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    if (day.equals(new Day(dataDate))) {
                        price = data.getClose();
                        break;
                    }
                }

                if (entry.getValue() == TradeSignal.BUY) {
                    buySeries.add(day, price);
                } else if (entry.getValue() == TradeSignal.SELL) {
                    sellSeries.add(day, price);
                }
            }

            // 添加买入/卖出信号到数据集
            signalDataset.addSeries(buySeries);
            signalDataset.addSeries(sellSeries);

            // 创建信号渲染器
            XYLineAndShapeRenderer signalRenderer = new XYLineAndShapeRenderer();
            signalRenderer.setSeriesPaint(0, Color.GREEN);  // 买入信号颜色
            signalRenderer.setSeriesPaint(1, Color.RED);    // 卖出信号颜色
            signalRenderer.setSeriesShapesVisible(0, true);
            signalRenderer.setSeriesShapesVisible(1, true);
            signalRenderer.setSeriesLinesVisible(0, false);
            signalRenderer.setSeriesLinesVisible(1, false);
            signalRenderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
            signalRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));

            // 添加第三个渲染器和数据集
            plot.setDataset(2, signalDataset);
            plot.setRenderer(2, signalRenderer);
        }

        // 显示图表
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1000, 600));

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        logger.info("Candlestick chart created and displayed: {}", title);
    }

    /**
     * 创建蜡烛图数据集
     */
    private static OHLCDataset createCandlestickDataset(List<StockData> stockData) {
        int size = stockData.size();
        Date[] dates = new Date[size];
        double[] opens = new double[size];
        double[] highs = new double[size];
        double[] lows = new double[size];
        double[] closes = new double[size];
        double[] volumes = new double[size];

        for (int i = 0; i < size; i++) {
            StockData data = stockData.get(i);
            dates[i] = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            opens[i] = data.getOpen();
            highs[i] = data.getHigh();
            lows[i] = data.getLow();
            closes[i] = data.getClose();
            volumes[i] = data.getVolume();
        }

        return new DefaultHighLowDataset(
                "Price",
                dates,
                highs,
                lows,
                opens,
                closes,
                volumes
        );
    }

    /**
     * 保存图表为图片文件
     */
    public static void saveChartAsImage(JFreeChart chart, String filePath, int width, int height) {
        try {
            org.jfree.chart.ChartUtils.saveChartAsPNG(
                    new java.io.File(filePath),
                    chart,
                    width,
                    height
            );
            logger.info("Chart saved to: {}", filePath);
        } catch (Exception e) {
            logger.error("Error saving chart: {}", e.getMessage());
        }
    }

    /**
     * 创建并保存价格图表
     */
    public static void createAndSaveChart(String title, List<StockData> stockData,
                                          Map<String, List<Double>> indicators,
                                          Map<Date, TradeSignal> signals,
                                          String filePath) {
        // 创建价格时间序列
        TimeSeries priceSeries = new TimeSeries("Price");

        // 添加价格数据
        for (StockData data : stockData) {
            Date date = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            priceSeries.add(new Day(date), data.getClose());
        }

        // 创建数据集合
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(priceSeries);

        // 添加指标数据
        for (Map.Entry<String, List<Double>> entry : indicators.entrySet()) {
            TimeSeries indicatorSeries = new TimeSeries(entry.getKey());
            List<Double> values = entry.getValue();

            for (int i = 0; i < values.size() && i < stockData.size(); i++) {
                Date date = Date.from(stockData.get(i).getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                indicatorSeries.add(new Day(date), values.get(i));
            }

            dataset.addSeries(indicatorSeries);
        }

        // 创建图表
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,      // 标题
                "Date",     // x轴标签
                "Price",    // y轴标签
                dataset,    // 数据
                true,       // 显示图例
                true,       // 显示工具提示
                false       // 不生成URL
        );

        // 自定义图表外观
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 自定义日期格式
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

        // 设置线条和形状渲染器
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // 价格线
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, false);

        // 设置指标线的颜色和粗细
        int index = 1;
        for (String key : indicators.keySet()) {
            Color color = (key.contains("Short")) ? Color.RED : Color.GREEN;
            renderer.setSeriesPaint(index, color);
            renderer.setSeriesStroke(index, new BasicStroke(1.5f));
            renderer.setSeriesShapesVisible(index, false);
            index++;
        }

        // 添加交易信号
        if (signals != null && !signals.isEmpty()) {
            // 与createPriceMAChart方法中的信号处理相同
            TimeSeries buySeries = new TimeSeries("Buy");
            TimeSeries sellSeries = new TimeSeries("Sell");

            for (Map.Entry<Date, TradeSignal> entry : signals.entrySet()) {
                Date date = entry.getKey();
                Day day = new Day(date);

                // 获取该日期的价格
                double price = 0;
                for (StockData data : stockData) {
                    Date dataDate = Date.from(data.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                    if (day.equals(new Day(dataDate))) {
                        price = data.getClose();
                        break;
                    }
                }

                if (entry.getValue() == TradeSignal.BUY) {
                    buySeries.add(day, price);
                } else if (entry.getValue() == TradeSignal.SELL) {
                    sellSeries.add(day, price);
                }
            }

            // 添加买入/卖出信号到数据集
            TimeSeriesCollection signalDataset = new TimeSeriesCollection();
            signalDataset.addSeries(buySeries);
            signalDataset.addSeries(sellSeries);

            // 创建信号渲染器
            XYLineAndShapeRenderer signalRenderer = new XYLineAndShapeRenderer();
            signalRenderer.setSeriesPaint(0, Color.GREEN);  // 买入信号颜色
            signalRenderer.setSeriesPaint(1, Color.RED);    // 卖出信号颜色
            signalRenderer.setSeriesShapesVisible(0, true);
            signalRenderer.setSeriesShapesVisible(1, true);
            signalRenderer.setSeriesLinesVisible(0, false);
            signalRenderer.setSeriesLinesVisible(1, false);
            signalRenderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
            signalRenderer.setSeriesShape(1, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));

            // 添加第二个渲染器和数据集
            plot.setDataset(1, signalDataset);
            plot.setRenderer(1, signalRenderer);
        }

        plot.setRenderer(0, renderer);

        // 保存图表为图片
        saveChartAsImage(chart, filePath, 1200, 800);
    }

    /**
     * 创建性能报告图表
     */
    public static void createPerformanceChart(String title, List<LocalDate> dates,
                                              Map<String, List<Double>> performances) {
        // 创建数据集合
        TimeSeriesCollection dataset = new TimeSeriesCollection();

        // 添加每个性能指标
        for (Map.Entry<String, List<Double>> entry : performances.entrySet()) {
            TimeSeries series = new TimeSeries(entry.getKey());
            List<Double> values = entry.getValue();

            for (int i = 0; i < values.size() && i < dates.size(); i++) {
                Date date = Date.from(dates.get(i).atStartOfDay(ZoneId.systemDefault()).toInstant());
                series.add(new Day(date), values.get(i));
            }

            dataset.addSeries(series);
        }

        // 创建图表
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,           // 标题
                "Date",          // x轴标签
                "Performance",   // y轴标签
                dataset,         // 数据
                true,            // 显示图例
                true,            // 显示工具提示
                false            // 不生成URL
        );

        // 自定义图表外观
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 自定义日期格式
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy-MM-dd"));

        // 设置线条和形状渲染器
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // 设置不同性能指标的颜色和样式
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        for (int i = 0; i < performances.size(); i++) {
            renderer.setSeriesPaint(i, colors[i % colors.length]);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, false);
        }

        plot.setRenderer(renderer);

        // 显示图表
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1000, 600));

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        logger.info("Performance chart created and displayed: {}", title);
    }
}