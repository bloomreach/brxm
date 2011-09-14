/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.testutils.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.hippoecm.testutils.history.myXML.myXMLEncodingException;
import org.hippoecm.testutils.history.myXML.myXMLException;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.statistics.Statistics;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ChartsCreator implements XmlConstants {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    public void createCharts(File[] historyFiles, File reportsDir) {
        for (int j = 0; j < historyFiles.length; j++) {
            try {
                reportsDir.mkdirs();
                createChart(historyFiles[j], reportsDir);
            } catch (myXMLException e) {
                e.printStackTrace();
            } catch (myXMLEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createChart(File historyFile, File reportsDir) throws myXMLException, myXMLEncodingException,
            IOException {
        BufferedReader reader = new BufferedReader(new FileReader(historyFile));
        myXML testSuite = new myXML(reader);
        if (testSuite != null) {
            String className = testSuite.Attribute.find(ATTR_CLASSNAME);
            for (int testcaseIndex = 0; testcaseIndex < testSuite.size(); testcaseIndex++) {
                myXML testCase = testSuite.getElement(testcaseIndex);
                CombinedDomainXYPlot plot = processTestCase(testCase);

                String testName = testCase.Attribute.find(ATTR_NAME);
                JFreeChart chart = new JFreeChart(testName, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
                chart.addSubtitle(new TextTitle(className));

                try {
                    File outputfile = new File(reportsDir, className + "_" + testName + "_history.png");
                    ChartUtilities.saveChartAsPNG(outputfile, chart, 800, 200 + 200 * testCase.size(), null, true, 9);
                } catch (IOException e) {
                    System.err.println("Exception while creating a performance charts image file: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private CombinedDomainXYPlot processTestCase(myXML testCase) {
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
        if (testCase != null) {
            for (int metricsIndex = 0; metricsIndex < testCase.size(); metricsIndex++) {
                XYPlot subPlot = processMetric(testCase.getElement(metricsIndex));
                plot.add(subPlot);
            }
        }

        DateAxis dateAxis = new DateAxis("Date");
        dateAxis.setDateFormatOverride(new SimpleDateFormat("dd-MMM (HH:mm)"));
        plot.setDomainAxis(dateAxis);
        plot.setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
        plot.setDomainGridlinesVisible(true);

        return plot;
    }

    private XYPlot processMetric(myXML metric) {
        XYPlot plot = new XYPlot();
        if (metric != null) {
            XYSeries xySeries = getSeries(metric);

            NumberAxis rangeAxis = new NumberAxis(metric.Attribute.find(ATTR_UNIT));
            rangeAxis.setAutoRangeIncludesZero(false);
            plot.setRangeAxis(rangeAxis);

            if (metric.Attribute.find(ATTR_FUZZY).equals("true")) {

                xySeries = removeExtremes(xySeries);

                XYDataset movingAverage = new XYSeriesCollection(createMovingAverage(xySeries, 20));
                plot.setDataset(1, movingAverage);
                plot.setRenderer(1, new StandardXYItemRenderer());

                XYDataset measurePoints = new XYSeriesCollection(xySeries);
                plot.setDataset(0, measurePoints);
                if (xySeries.getItemCount() < 20) {
                    plot.setRenderer(0, new StandardXYItemRenderer());
                } else {
                    plot.setRenderer(0, new XYDotRenderer());
                }

            } else {
                XYDataset measurePoints = new XYSeriesCollection(xySeries);
                plot.setDataset(0, measurePoints);
                plot.setRenderer(0, new StandardXYItemRenderer());
            }
        }
        return plot;
    }

    private XYSeries getSeries(myXML metric) {
        XYSeries result = new XYSeries(metric.Attribute.find(ATTR_NAME));

        OrderedMap map = new ListOrderedMap();
        for (int measurepointIndex = 0; measurepointIndex < metric.size(); measurepointIndex++) {
            myXML measurepoint = metric.getElement(measurepointIndex);
            Double timestamp = new Double(measurepoint.Attribute.find(ATTR_TIMESTAMP));
            Double value = new Double(measurepoint.Attribute.find(ATTR_VALUE));
            Set set = (Set) map.get(timestamp);
            if (set == null) {
                set = new HashSet();
                map.put(timestamp, set);
            }
            set.add(value);
        }

        MapIterator it = map.mapIterator();
        while (it.hasNext()) {
            double timestamp = ((Double) it.next()).doubleValue();
            Set values = (Set) it.getValue();
            double mean = Statistics.calculateMean(values);
            result.add(timestamp, mean);
        }
        return result;
    }

    private XYSeries createMovingAverage(XYSeries xySeries, int trail) {
        int itemCount = xySeries.getItemCount();
        Double[] xData = new Double[itemCount];
        Double[] yData = new Double[itemCount];
        for (int i = 0; i < itemCount; i++) {
            xData[i] = new Double(xySeries.getX(i).doubleValue());
            yData[i] = new Double(xySeries.getY(i).doubleValue());
        }

        double[][] movingAverage = Statistics.getMovingAverage(xData, yData, itemCount > trail ? trail : itemCount);

        XYSeries result = new XYSeries("Moving average(" + trail + ")");
        for (int i = 0; i < movingAverage.length; i++) {
            result.add(movingAverage[i][0], movingAverage[i][1]);
        }
        return result;
    }

    private XYSeries removeExtremes(XYSeries series) {
        int itemCount = series.getItemCount();
        Double[] yData = new Double[itemCount];
        for (int i = 0; i < itemCount; i++) {
            yData[i] = new Double(series.getY(i).doubleValue());
        }

        double stdDev = Statistics.getStdDev(yData);
        double mean = Statistics.calculateMean(yData);

        XYSeries result = new XYSeries("Measure points");
        for (int i = 0; i < yData.length; i++) {
            if (series.getY(i).doubleValue() > mean - 2 * stdDev && series.getY(i).doubleValue() < mean + 2 * stdDev) {
                result.add(series.getX(i), series.getY(i));
            }
        }
        return result;
    }

}
