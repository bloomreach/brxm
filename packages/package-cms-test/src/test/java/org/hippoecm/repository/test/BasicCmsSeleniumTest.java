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

import org.junit.Test;

public class BasicCmsSeleniumTest extends CmsSeleniumTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Test logging in and out as admin, editor and author 
     * @throws Exception
     */
    @Test
    public void testLoginLogout() throws Exception {
        executeTest(new SeleniumTest() {
            public void execute() throws Exception {
                doOpenHomepage();
                
                doLogin("admin", "admin");
                doLogout();

                doLogin("editor", "editor");
                doLogout();

                doLogin("author", "author");
                doLogout();
            }
        });
    }
    
    /**
     * Test editing a document with user author
     * @throws Exception
     */
    @Test
    public void testSimpleEdit() throws Exception {
        executeTest(new SeleniumTest() {
            public void execute() throws Exception {
                
                doOpenHomepage();
                doLogin("author", "author");


                //switch to documents perspective
                clickAndWaitForText("home.cluster.cms-static.plugin.centerPlugin.service.wicket.id_6", new String[] {
                        "articles", "Folders", "Browse" },
                        "Timeout loading documents perspective");

                //select articles folder
                clickAndWaitForText(
                        "//a[@id='home.cluster.cms-static.plugin.documentsTreeLoader.cluster.documents.plugin.documentsBrowser.service.wicket.id_3']/span[2]",
                        new String[] { "myarticle1", "myarticle2" },
                        "Timeout loading articles folder content");

                //select 'myarticle2' document
                clickAndWaitForText(
                        "//span[@id='home.cluster.cms-static.plugin.browserPerspective.cluster.hippostd_folder.plugin.root.service.wicket.id_11']/span",
                        new String[] { "unknown document", "new" }, "Timeout loading myarticle2 preview");

                //start editing 'myarticle2'
                clickAndWaitForText(
                        "//a[@id='home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-preview.plugin.reviewedActionWorkflowPlugin.cluster..plugin.home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-preview.plugin.home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-preview.plugin.workflow.options.service.wicket.id_1']/span",
                        "Save and close", "Timeout while opening myarticle2 in edit-modus");

                //Change document title to 'title-testvalue-myarticle2'
                final String newTitle = "title-testvalue-myarticle2";
                selenium
                        .type(
                                "home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.editorPlugin.cluster.hippo_template.plugin.title.cluster.hippo_template.plugin.root.service.wicket.id_1",
                                newTitle);

                //Press save&close
                selenium
                    .click("//a[@id='home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.editWorkflowPlugin.cluster..plugin.home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.workflow.options.service.wicket.id_1']/span");


                //First click on the save button blurs title field, which 
                //  1) cancels the save action 
                //  2) sends the new input value to the server 
                //  3) returns a new save link html
                //so let it load
                Thread.sleep(2500);
                //Now do the real save
                selenium
                    .click("//a[@id='home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.editWorkflowPlugin.cluster..plugin.home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.workflow.options.service.wicket.id_3']/span");

                //Wait for 5 seconds until editor is closed by checking if the input field is gone
                String titleInputNotFoundScript = "selenium.browserbot.getCurrentWindow().document.getElementById('home.cluster.cms-static.plugin.editorManagerPlugin.cluster.cms-editor.plugin.editorPlugin.cluster.hippo:template.plugin.title.cluster.hippo:template.service.wicket.id_2') == null";
                selenium.waitForCondition(titleInputNotFoundScript, "3000");

                //select myarticle1 and than myarticle2 again to load the preview again
                selenium
                        .click("//span[@id='home.cluster.cms-static.plugin.browserPerspective.cluster.hippostd_folder.plugin.root.service.wicket.id_8']/span");

                Thread.sleep(2500l);
                
                clickAndWaitForText(
                        "//span[@id='home.cluster.cms-static.plugin.browserPerspective.cluster.hippostd_folder.plugin.root.service.wicket.id_11']/span",
                        newTitle, "Timeout while loading new preview of 'myarticle2' with title " + newTitle);

                doLogout();
            }
        });
    }
    
    /**
     * Test creation of a new user, add it to the author group and try do login as new user
     * @throws Exception
     */
    @Test
    public void testNewUser() throws Exception {
        executeTest(new SeleniumTest() {
            public void execute() throws Exception {

                doOpenHomepage();
                doLogin("admin", "admin");

                //open usermanagement perspective
                clickAndWaitForText("home.cluster.cms-static.plugin.centerPlugin.service.wicket.id_8", "Control panel",
                        "Timeout loading user management perspective");

                //click on users
                clickAndWaitForText("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_1",
                        new String[] { "Users", "Create user" }, "Timeout loading user panel");

                //click on 'create user'
                clickAndWaitForText("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_8",
                        "Please fill in the details of the new user", "Timeout loading new user form");

                //enter user data
                String[] userData = new String[] { "testuser", "firstname-testuser", "lastname-testuser",
                        "testuser@mail.com", "testtest" };

                selenium.type("username", userData[0]);
                selenium.type("firstName", userData[1]);
                selenium.type("lastName", userData[2]);
                selenium.type("email", userData[3]);
                selenium.type("password", userData[4]);
                selenium.type("password-check", userData[4]);

                //save user and verify if user exists by looking up email
                clickAndWaitForText("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_19",
                        userData[3], "Failed to create new user");

                //Click on new user in list
                clickAndWaitForText("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_24",
                        "This user is member of the following groups", "Timout loading edit user panel");

                clickAndWaitForText("link=Set group memberships", "Edit user group membership",
                        "Timout loading user group membership panel");

                selenium.select("local-groups", "label=author");

                clickAndWaitForText("home.cluster.cms-static.plugin.adminPerspective.service.wicket.id_31", "author",
                        "Failed to add testuser to group author");

                doLogout();

                //test if new user can login
                doLogin(userData[0], userData[4]);
                doLogout();

                //TODO: remove user
            }
        });
    }
}
