package org.hippoecm.repository.test;

public class CmsSeleniumTest extends BasicSeleniumTest {

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
