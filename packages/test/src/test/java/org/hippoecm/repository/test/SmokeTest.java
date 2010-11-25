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

import com.thoughtworks.selenium.DefaultSelenium;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SmokeTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected DefaultSelenium selenium;

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
        selenium.open("http://localhost:4849/");

        assert (selenium.isTextPresent("Username"));
        assert (selenium.isTextPresent("Password"));
        
        selenium.type("home.cluster.login.plugin.loginPage.service.wicket.id_2", "admin");
        selenium.type("home.cluster.login.plugin.loginPage.service.wicket.id_4", "admin");
        selenium.click("home.cluster.login.plugin.loginPage.service.wicket.id_7");
        selenium.waitForPageToLoad("5000");
        //Thread.sleep(5000);

        selenium.click("home.cluster.cms-static.plugin.centerPlugin.service.wicket.id_8");
        Thread.sleep(5000); //selenium.waitForPageToLoad("5000");
        selenium.click("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_1");
        Thread.sleep(5000); //selenium.waitForPageToLoad("5000");
        selenium.click("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_7");
        Thread.sleep(5000); //selenium.waitForPageToLoad("5000");
        selenium.type("username", "test");
        selenium.type("firstName", "test");
        selenium.type("lastName", "test");
        selenium.type("email", "test@hippo.nl");
        selenium.type("password", "u1cc0");
        selenium.type("password-check", "u1cc0");
        selenium.click("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_17");
        //Thread.sleep(5000);

        selenium.click("home.cluster.cms-static.plugin.logoutPlugin.service.wicket.id_1");
        selenium.waitForPageToLoad("5000");

        selenium.type("home.cluster.login.plugin.loginPage.service.wicket.id_2", "author");
        selenium.type("home.cluster.login.plugin.loginPage.service.wicket.id_4", "author");
        selenium.click("home.cluster.login.plugin.loginPage.service.wicket.id_7");
        selenium.waitForPageToLoad("5000");
        Thread.sleep(5000);
        //refresh();
        selenium.click("home.cluster.cms-static.plugin.centerPlugin.service.wicket.id_6");
        Thread.sleep(5000);
        selenium.click("//span[@id='home.cluster.cms-static.plugin.documentsTreeLoader.cluster.documents.plugin.documentsBrowser.service.wicket.id_10']/a");
        Thread.sleep(5000);
        selenium.click("//span[@id='home.cluster.cms-static.plugin.documentsTreeLoader.cluster.documents.plugin.module.workflow.service.wicket.id_2']/span");
        Thread.sleep(5000);
        selenium.type("root_4", "test");
        //selenium.click("//form[@id='root_5']/table/tbody/tr[1]/td[2]");
        selenium.select("prototype", "label=Event");
        selenium.click("root_7");
        Thread.sleep(5000);
        selenium.type("home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.editorPlugin.cluster._default_.plugin.title.cluster._default_.plugin.root.service.wicket.id_1", "title aap");
        selenium.click("//div[@id='home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.editorPlugin.cluster._default_.plugin.date.service.wicket.id']/div");
        Thread.sleep(5000);
        selenium.click("//span[@id='home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.workflowPlugin.service.wicket.id_4']/span");
        Thread.sleep(5000);

        selenium.click("home.cluster.cms-static.plugin.logoutPlugin.service.wicket.id_1");
        selenium.waitForPageToLoad("5000");
    }
}
