package org.hippoecm.repository.testutils.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.hippoecm.repository.testutils.history.myXML.myXMLEncodingException;
import org.hippoecm.repository.testutils.history.myXML.myXMLException;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;

public class ChartsCreator extends Task implements XmlConstants {

    private static final String DATEFORMAT = "dd-MM-yyyy hh:mm:ss";

    //private myXML testSuite;
    private String reportsDir;
    private Vector filesets = new Vector();

    public void setDir(String dir) {
        this.reportsDir = dir;
    }

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    public void execute() {
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int j = 0; j < srcFiles.length; j++) {
                try {
                    createChart(new File(fs.getDir(getProject()), srcFiles[j]));
                } catch (myXMLException e) {
                    e.printStackTrace();
                } catch (myXMLEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createChart(File file) throws myXMLException, myXMLEncodingException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
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
                    File outputfile = getImageFile(reportsDir, className, testName);
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
        dateAxis.setDateFormatOverride(new SimpleDateFormat(DATEFORMAT));
        plot.setDomainAxis(dateAxis);
        plot.setDomainGridlinesVisible(true);

        return plot;
    }

    private XYPlot processMetric(myXML metric) {
        XYPlot plot = new XYPlot();
        if (metric != null) {

            TimeSeries timeseries = new TimeSeries(metric.Attribute.find(ATTR_NAME), Millisecond.class);
            for (int measurepointIndex = 0; measurepointIndex < metric.size(); measurepointIndex++) {
                myXML measurepoint = metric.getElement(measurepointIndex);
                Date timestamp = new Date(Long.parseLong(measurepoint.Attribute.find(ATTR_TIMESTAMP)));
                Number value = new Double(measurepoint.Attribute.find(ATTR_VALUE));
                timeseries.add(new Millisecond(timestamp), value);
            }

            NumberAxis rangeAxis = new NumberAxis(metric.Attribute.find(ATTR_UNIT));
            rangeAxis.setAutoRangeIncludesZero(false);
            plot.setRangeAxis(rangeAxis);

            if (metric.Attribute.find(ATTR_FUZZY).equals("true")) {
                XYDataset timeSeriesMeasurePoints = new TimeSeriesCollection(timeseries);
                plot.setDataset(0, timeSeriesMeasurePoints);
                plot.setRenderer(0, new XYDotRenderer());

                XYDataset timeSeriesMovingAverage = new TimeSeriesCollection(createMovingAverage(timeseries, 20));
                plot.setDataset(1, timeSeriesMovingAverage);
                plot.setRenderer(1, new StandardXYItemRenderer());
            } else {
                XYDataset timeSeriesMeasurePoints = new TimeSeriesCollection(timeseries);
                plot.setDataset(0, timeSeriesMeasurePoints);
                plot.setRenderer(0, new StandardXYItemRenderer());
            }
        }
        return plot;
    }

    private File getImageFile(String basedir, String suiteName, String testName) {
        int packageDepth = suiteName.lastIndexOf('.');
        String packageName = suiteName.substring(0, packageDepth == -1 ? 0 : packageDepth);
        String className = suiteName.substring(packageDepth + 1, suiteName.length());
        String dirname = basedir + File.separator + packageName.replaceAll("\\.", "/");

        File dir = new File(dirname);
        dir.mkdirs();
        return new File(dir, className + "_" + testName + "_history.png");
    }

    private TimeSeries createMovingAverage(TimeSeries timeseries, int trail) {
        TimeSeries result = new TimeSeries("Moving average(" + trail + ")", Millisecond.class);

        double value = 0.0d;
        int count = 0;
        for (int i = 0; i < timeseries.getItemCount(); i++) {
            TimeSeriesDataItem item = timeseries.getDataItem(i);
            value += item.getValue().doubleValue();
            if (i >= trail) {
                value -= timeseries.getDataItem(i - trail).getValue().doubleValue();
            } else {
                count++;
            }

            result.add(new Millisecond(new Date(item.getPeriod().getMiddleMillisecond())), value / count);
        }
        return result;
    }

}
