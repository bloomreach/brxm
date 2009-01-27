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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.hippoecm.testutils.history.myXML.myXMLException;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;

public class HistoryWriter extends Suite implements XmlConstants {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private File historyFile;

    private myXML testSuite;
    private myXML testCase;

    private long timeStamp;

    private String history;

    public HistoryWriter(Class<?> klass) throws InitializationError {
        super(klass);
        this.timeStamp = System.currentTimeMillis();
        this.history = System.getProperty("history.points");
        history = (history == null ? null : history.startsWith("${") ? null : history);
    }

    public static HistoryWriter currentHistoryWriter;

    @Override
    public void run(final RunNotifier notifier) {
        currentHistoryWriter = this;
        Listener listener = null;
        if (history != null) {
            listener = new Listener();
            notifier.addListener(listener);
            try {
                historyFile = new File(history, getDescription().getDisplayName() + "-history.xml");
                if (historyFile.exists()) {
                    testSuite = new myXML(new BufferedReader(new FileReader(historyFile)));
                } else {
                    testSuite = new myXML(TESTSUITE);
                }
                testSuite.Attribute.add(ATTR_CLASSNAME, getDescription().getDisplayName());
            } catch (Exception e) {
                System.err.println("HistoryWriter.setUp failed: " + e.getMessage());
            }

        }
        super.run(notifier);
        if (listener != null) {
            OutputStream out = null;
            try {
                historyFile.getParentFile().mkdirs();
                out = new FileOutputStream(historyFile);
            } catch (IOException e) {
                System.err.println("Cannot open file " + historyFile.getAbsolutePath());
            }
            if (out != null) {
                PrintWriter writer = null;
                try {
                    writer = new PrintWriter(new OutputStreamWriter(out, "UTF8"));
                    testSuite.serialize(writer);
                } catch (IOException exc) {
                    System.err.println("Cannot write to history file " + historyFile.getAbsolutePath() + ": "
                            + exc.getMessage());
                } finally {
                    if (out != System.out && out != System.err && writer != null) {
                        writer.close();
                    }
                }
            }
        }
    }

    class Listener extends RunListener {
        @Override
        public void testRunStarted(Description description) throws Exception {
        }
        @Override
        public void testRunFinished(Result result) throws Exception {
        }
        @Override
        public void testStarted(Description description) throws Exception {
            try {
                String name = description.getDisplayName();
                if(name.contains("("))
                    name = name.substring(0,name.indexOf("("));
                testCase = testSuite.findElement(TESTCASE, ATTR_NAME, name);
                if (testCase == null) {
                    testCase = testSuite.addElement(TESTCASE);
                }
                testCase.Attribute.add(ATTR_NAME, name);
            } catch (myXMLException e) {
                System.err.println("Failed to add testcase data to testsuite: " + e.getMessage());
            }
        }
        @Override
        public void testFinished(Description description) throws Exception {
            if (testCase.isEmpty()) {
                String name = description.getDisplayName();
                if(name.contains("("))
                    name = name.substring(0,name.indexOf("("));
                try {
                    testSuite.removeElement(testCase);
                } catch (myXMLException e) {
                    System.err.println("Unable to remove empty testcase " + name + " from suite");
                }
            }
        }
        @Override
        public void testFailure(Failure failure) throws Exception {
        }
        @Override
        public void testIgnored(Description description) throws Exception {
        }
    }

    public static void write(String name, String value, String unit) {
        if (currentHistoryWriter != null) {
            currentHistoryWriter.writeInternal(name, value, unit, true);
        }
    }

    public static void write(String name, String value, String unit, boolean fuzzy) {
        if (currentHistoryWriter != null) {
            currentHistoryWriter.writeInternal(name, value, unit, fuzzy);
        }
    }

    public void writeInternal(String name, String value, String unit, boolean fuzzy) {
        if (history != null) {
            try {
                myXML metric = testCase.findElement(METRIC, ATTR_NAME, name);
                if (metric == null) {
                    metric = testCase.addElement(METRIC);
                }
                metric.Attribute.add(ATTR_NAME, name);
                metric.Attribute.add(ATTR_UNIT, unit);
                metric.Attribute.add(ATTR_FUZZY, String.valueOf(fuzzy));

                myXML measurePoint = metric.addElement(MEASUREPOINT);
                measurePoint.Attribute.add(ATTR_TIMESTAMP, String.valueOf(timeStamp));
                measurePoint.Attribute.add(ATTR_VALUE, value);
            } catch (myXMLException e) {
                System.err.println("Failed to add testcase data to testsuite: " + e.getMessage());
            }
        }
    }
}
