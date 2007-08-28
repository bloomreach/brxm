package org.hippoecm.repository.testutils.history;

/**
 * XmlConstants.java
 */
public interface XmlConstants {

  // the testsuite element
  public final static String TESTSUITE = "testsuite";

  // classname attribute for testsuite elements
  public final static String ATTR_CLASSNAME = "classname";
  
  // the testcase element
  public final static String TESTCASE = "testcase";

  // name attribute for testcase and metric elements
  public final static String ATTR_NAME = "name";

  // the metric element
  public final static String METRIC = "metric";

  // unit attribute for metric elements
  public final static String ATTR_UNIT = "unit";

  // fuzzy attribute for metric elements
  public final static String ATTR_FUZZY = "fuzzy";

  // the measurepoint element
  public final static String MEASUREPOINT = "measurepoint";

  // timestamp attributes for measurepoint elements
  public final static String ATTR_TIMESTAMP = "timestamp";
  
  // value attribute for measurepoint elements
  public final static String ATTR_VALUE = "value";
  
  // skip attribute for measurepoint elements
  public final static String ATTR_SKIP = "skip";

}
