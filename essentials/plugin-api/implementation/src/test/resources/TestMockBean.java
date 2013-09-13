/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: TestMockBean.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
public class TestMockBean {

    private static Logger log = LoggerFactory.getLogger(TestMockBean.class);


    public static String existingStaticMethod() {
        return "";
    }


    public String existingMethod() {
        return "";
    }

}
