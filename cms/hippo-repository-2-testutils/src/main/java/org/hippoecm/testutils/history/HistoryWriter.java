/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
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
import java.lang.reflect.Method;

import org.hippoecm.testutils.history.myXML.myXMLException;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

public class HistoryWriter extends TestSetup implements TestListener, XmlConstants {

    private File historyFile;

    private myXML testSuite;
    private myXML testCase;

    private String name;
    private long timeStamp;

    private String history;

    public HistoryWriter(Test test) {
        super(test);
        this.name = getName(test);
        this.timeStamp = System.currentTimeMillis();
        this.history = System.getProperty("history.points");
        history = history == null ? null : history.startsWith("${") ? null : history;
    }

    public String getName() {
        return name;
    }

    public void run(final TestResult result) {
        if (history != null) {
            result.addListener(this);
        }
        super.run(result);
    }

    public void setUp() {
        if (history != null) {
            try {
                historyFile = new File(history, getName() + "-history.xml");
                if (historyFile.exists()) {
                    testSuite = new myXML(new BufferedReader(new FileReader(historyFile)));
                } else {
                    testSuite = new myXML(TESTSUITE);
                }
                testSuite.Attribute.add(ATTR_CLASSNAME, getName());
            } catch (Exception e) {
                System.err.println("HistoryWriter.setUp failed: " + e.getMessage());
            }
        }
    }

    public void tearDown() {
        if (history != null) {
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

    public void write(String name, String value, String unit) {
        write(name, value, unit, true);
    }

    public void write(String name, String value, String unit, boolean fuzzy) {
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

    // interface TestListener

    public void startTest(Test test) {
        try {
            testCase = testSuite.findElement(TESTCASE, ATTR_NAME, getName(test));
            if (testCase == null) {
                testCase = testSuite.addElement(TESTCASE);
            }
            testCase.Attribute.add(ATTR_NAME, getName(test));
        } catch (myXMLException e) {
            System.err.println("Failed to add testcase data to testsuite: " + e.getMessage());
        }
    }

    public void endTest(Test test) {
        if (testCase.isEmpty()) {
            try {
                testSuite.removeElement(testCase);
            } catch (myXMLException e) {
                System.err.println("Unable to remove empty testcase " + getName(test) + " from suite " + getName());
            }
        }
    }

    public void addError(Test test, Throwable t) {
    }

    public void addFailure(Test test, AssertionFailedError t) {
    }

    // privates

    private String getName(Test t) {
        try {
            Method getNameMethod = t.getClass().getMethod("getName", new Class[0]);
            if (getNameMethod != null && getNameMethod.getReturnType() == String.class) {
                return (String) getNameMethod.invoke(t, new Object[0]);
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }

}
