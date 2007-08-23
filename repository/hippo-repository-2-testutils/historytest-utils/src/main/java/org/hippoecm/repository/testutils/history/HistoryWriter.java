package org.hippoecm.repository.testutils.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

import org.hippoecm.repository.testutils.history.myXML.myXMLException;


public class HistoryWriter extends TestSetup implements TestListener, XmlConstants {

  private File historyFile;
  
  private myXML testSuite;
  private myXML testCase;  
  
  private Set loggedTo;
  private String name;
  private long timeStamp;
  
  private Configuration config;

 
  public HistoryWriter(Test test) {
    super(test);
    this.name = getName(test);
    this.timeStamp = System.currentTimeMillis();
    this.config = Configuration.getInstance();
  }
  
  
  public String getName() {
    return name;
  }


  public void run(final TestResult result) {
    result.addListener(this);
    super.run(result);
  }


  public void setUp() {
    try {
      historyFile = new File(config.getHistoryDir(), getName() + "-history.xml");
      
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
    loggedTo = new HashSet();
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


  public void tearDown() {
    writeToHistoryFile();
    new ChartsCreator(testSuite).execute();
  }


  public void write(String name, String value, String unit) {
    write(name, value, unit, true);
  }


  public void write(String name, String value, String unit, boolean fuzzy) {
    if (loggedTo != null) {
      if (!loggedTo.add(name)) {
        System.out.println("Duplicate log name: " + name
            + ", it is not allowed to log to the same name more than once in one testmethod.");
        return;
      }
      
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
  
  
  public void addError(Test test, Throwable t) {
  }

  public void addFailure(Test test, AssertionFailedError t) {
  }

  private void writeToHistoryFile() {
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
        System.err.println("Unable to history file" + exc.getMessage());
      } finally {
        if (out != System.out && out != System.err && writer != null) {
          writer.close();
        }
      }
    }
  }

  private String getName(Test t) {
    try {
       Method getNameMethod = t.getClass().getMethod("getName", new Class [0]);
        if (getNameMethod != null && getNameMethod.getReturnType() == String.class) {
            return (String) getNameMethod.invoke(t, new Object[0]);
        }
    } catch (Throwable e) {
        // ignore
    }
    return "unknown";
  }

}
