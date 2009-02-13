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

public class CmsSeleniumTest extends BasicSeleniumTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public void doOpenHomepage() {
        selenium.open("http://localhost:4849/");
    }
    
    public void doLogin(String username, String password) throws Exception {
        assert (selenium.isTextPresent("Username"));
        assert (selenium.isTextPresent("Password"));
        
        // enter username and password
        selenium.type("home.cluster.login.plugin.loginPage.service.wicket.id_3", username);
        selenium.type("home.cluster.login.plugin.loginPage.service.wicket.id_5", password);

        // click ok
        clickAndWaitForText("home.cluster.login.plugin.loginPage.service.wicket.id_7", "Welcome to Hippo CMS 7",
                "Timeout for user " + username + " during login", 60);
    }

    public void doLogout() throws Exception {
        clickAndWaitForText("home.cluster.cms-static.plugin.logoutPlugin.service.wicket.id_1", "Password",
                "Timeout logging out");
    }
    
}
