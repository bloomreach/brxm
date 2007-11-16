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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions.selenium;

//import org.hippoecm.testutils.integration.HippoEcmSetup;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;

public class BasicTest extends TestCase {
    private DefaultSelenium selenium;

    // TODO implement suite() method returning a TestSetup which starts a repository and frontend
//    public static Test suite() throws Exception {
//        Test suite = new TestSuite(BasicTest.class);
//        HippoEcmSetup setup = new HippoEcmSetup(suite);
//        return setup;
//    }
    
    @Override
    public void setUp() throws Exception {
        selenium = createSeleniumClient("http://localhost:8082/");
        selenium.start();
    }

    @Override
    public void tearDown() throws Exception {
        selenium.stop();
    }

    protected DefaultSelenium createSeleniumClient(String url) throws Exception {
        return new DefaultSelenium("localhost", 4444, "*firefox", url);
    }

    /**
     * Checks if the "Add Node" menu action is present on the page.
     */
    public void testDummy() throws Exception {
        try {
            selenium.open("/");
            assertEquals("Add Node", selenium.getText("//a[@id='node_dialog_link4']"));
        } 
        catch (SeleniumException ex) {
            fail(ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * Browses through the tree to a document 'my article' and checks if it has the
     * workflow action "obtain editable instance".
     */
    public void testMyArticleHasWorkflow() throws Exception {
        try {
            selenium.open("/");

            selenium.click("//div[6]/div/a[1]/span/span");
            for (int second = 0;; second++) {
                if (second >= 60) fail("timeout");
                try { if (selenium.isElementPresent("//div[7]/div/a[2]/span[2]")) break; } catch (Exception e) {}
                Thread.sleep(1000);
            }

            selenium.click("//div[7]/div/a[1]/span/span");
            for (int second = 0;; second++) {
                if (second >= 60) fail("timeout");
                try { if (selenium.isElementPresent("//div[8]/div/a[2]/span[2]")) break; } catch (Exception e) {}
                Thread.sleep(1000);
            }

            selenium.click("//div[8]/div/a[2]/span[2]");
            for (int second = 0;; second++) {
                if (second >= 60) fail("timeout");
                try { if (selenium.isElementPresent("obtainEditableInstance62")) break; } catch (Exception e) {}
                Thread.sleep(1000);
            }

            assert(selenium.isTextPresent("Obtain editable copy"));
        assert(selenium.isTextPresent("Obtain editable copy"));
        } 
        catch (SeleniumException ex) {
            fail(ex.getMessage());
            throw ex;
        }
    }
    
    
    /**
     * Opens the frontend in a browser and logs in as user "demo".
     */
    public void testLogin() throws Exception {
        selenium.open("/");
        assert(selenium.isTextPresent("Logged in as anonymous"));
        selenium.click("login_dialog_link21");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("ok8")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.click("label4");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("editor10")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.type("editor10", "demo");
        selenium.keyPress("editor10", "\\13");
        selenium.click("label6");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("editor11")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.type("editor11", "demo");
        selenium.keyPress("editor11", "\\13");
        selenium.click("ok8");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isTextPresent("Logged in as demo")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }
    }
    
    /**
     * Browses through the tree and adds a node.
     */
    public void testAddNode() throws Exception {
        selenium.open("/");

        selenium.click("//div[6]/div/a[1]/span/span");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("//div[7]/div/a[1]/span/span")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.click("//div[7]/div/a[1]/span/span");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("//div[7]/div/a[2]/span[2]")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.click("//div[7]/div/a[2]/span[2]");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isTextPresent("/workflow-demo/myarticle")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.click("node-dialog-link4");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("ok8")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.click("label4");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("editor10")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.type("editor10", "test");
        selenium.keyPress("editor10", "\\13");
        selenium.click("ok8");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isElementPresent("//div[9]/div/a[2]/span[2]")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        selenium.click("//div[9]/div/a[2]/span[2]");
        for (int second = 0;; second++) {
            if (second >= 60) fail("timeout");
            try { if (selenium.isTextPresent("/workflow-demo/myarticle/test")) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }

        assert(selenium.isTextPresent("/workflow-demo/myarticle/test"));
        
    }
    
}