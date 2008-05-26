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
package org.hippoecm.repository.test;

import org.junit.*;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import static org.junit.Assert.*;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;

public class BasicSeleniumTest {
    private DefaultSelenium selenium;

    @Before
    public void setUp() throws Exception {
        selenium = new DefaultSelenium("localhost", 4444, "*firefox", "http://localhost:4849/");
        selenium.start();
    }

    @After
    public void tearDown() throws Exception {
        selenium.stop();
    }

    @Test
    public void testLoginLogout() throws Exception {
        doLogin();
        doLogout();
    }

    public void doLogin() throws Exception {
        try {
            selenium.open("http://localhost:4849/");
            assert(selenium.isTextPresent("Username"));
            assert(selenium.isTextPresent("Password"));

            // enter username
            selenium.type("username3", "admin");

            // enter password
            selenium.type("password4", "admin");

            // click ok
            selenium.click("submit5");
            for (int second = 0;; second++) {
                if (second >= 60) fail("timeout");
                try { if (selenium.isTextPresent("Dashboard")) break; } catch (Exception e) {}
                Thread.sleep(1000);
            }
        } catch (SeleniumException ex) {
            fail(ex.getMessage());
            throw ex;
        }
    }

    public void doLogout() throws Exception {
        try {
            selenium.click("logout_link9");
            for (int second = 0;; second++) {
                if (second >= 60) fail("timeout");
                try { if (selenium.isTextPresent("Password")) break; } catch (Exception e) {}
                Thread.sleep(1000);
            }
        } catch (SeleniumException ex) {
            fail(ex.getMessage());
            throw ex;
        }
    }
}
