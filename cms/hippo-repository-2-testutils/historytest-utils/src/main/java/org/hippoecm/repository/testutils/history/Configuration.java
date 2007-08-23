package org.hippoecm.repository.testutils.history;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class Configuration {
    
  private static final String DEFAULT_CONFIGURATION = "history.properties";
    
  
  private Properties buildProps = null;
  private static Configuration env;
  
  // singleton
  public static Configuration getInstance() {
    if (env == null) {
      env = new Configuration();
    }
    return env;
  }

  //private constructor, called by getInstance()
  private Configuration() {
    Properties defaultBuildProps = new Properties();
    try {
      defaultBuildProps.load(new FileInputStream(DEFAULT_CONFIGURATION));
    } catch (IOException e) {
    }

    buildProps = new Properties();
    buildProps.putAll(defaultBuildProps);
    buildProps.putAll(System.getProperties());
  }

  public String getResultsDir() {
      return buildProps.getProperty("results");
    }  
  
  public String getReportsDir() {
    return buildProps.getProperty("reports");
  }

  public String getHistoryDir() {
    return buildProps.getProperty("history");
  }

}
