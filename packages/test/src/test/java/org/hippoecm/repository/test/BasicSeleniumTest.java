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
package org.hippoecm.repository.test;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;

public class BasicSeleniumTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final int DEFAULT_MAX_SECONDS = 20;

    protected DefaultSelenium selenium;
    
    public interface SeleniumTest {
        void execute() throws Exception;
    }

    protected DefaultSelenium createDefaultSelenium() {
        return new DefaultSelenium("localhost", 4444, "*firefox", "http://localhost:4849/");
    }

    @Before
    public void setUp() throws Exception {
        selenium = createDefaultSelenium();
        selenium.start();
    }

    @After
    public void tearDown() throws Exception {
        selenium.stop();
    }
    
    protected void executeTest(SeleniumTest test) throws Exception {
        try {
            test.execute();
        } catch (SeleniumException ex) {
            fail(ex.getMessage());
            throw ex;
        }
    }

    protected void clickAndWaitForText(String element, String text, String fail) throws InterruptedException {
        selenium.click(element);
        waitForTextPresent(text, fail);
    }

    protected void clickAndWaitForText(String element, String text, String fail, int maxSeconds) throws InterruptedException {
        selenium.click(element);
        waitForTextPresent(text, fail, maxSeconds);
    }

    protected void clickAndWaitForText(String element, String[] text, String fail) throws InterruptedException {
        selenium.click(element);
        waitForAllTextPresent(text, fail);
    }
    
    protected void clickAndWaitForText(String element, String[] text, String fail, int maxSeconds)
            throws InterruptedException {
        selenium.click(element);
        waitForAllTextPresent(text, fail, maxSeconds);
    }
    
    protected void waitForTextPresent(String text, String fail) throws InterruptedException {
        waitForAllTextPresent(new String[] { text }, fail, DEFAULT_MAX_SECONDS);
    }

    protected void waitForTextPresent(String text, String fail, int maxSeconds) throws InterruptedException {
        waitForAllTextPresent(new String[] { text }, fail, maxSeconds);
    }    

    protected void waitForAllTextPresent(String[] text, String fail) throws InterruptedException {
        waitForAllTextPresent(text, fail, DEFAULT_MAX_SECONDS);
    }
    
    protected void waitForAllTextPresent(String[] text, String fail, int maxSeconds) throws InterruptedException {
        for (int second = 0;; second++) {
            if (second >= maxSeconds) {
                fail(fail);
            }
            try {
                int found = 0;
                for (int i = 0; i < text.length; i++) {
                    if (selenium.isTextPresent(text[i])) {
                        found++;
                    }
                }
                if (found == text.length)
                    break;
            } catch (Exception e) {
            }
            Thread.sleep(1000);
            
        }
    }
}
