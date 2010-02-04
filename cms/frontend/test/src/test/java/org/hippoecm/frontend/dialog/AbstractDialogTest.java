/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Before;
import org.junit.Test;

public class AbstractDialogTest {

    public static class Dialog extends AbstractDialog {

        boolean clicked = false;
        
        @Override
        protected void onOk() {
            if (clicked) {
                throw new RuntimeException("Error: don't call onOk twice");
            }
            clicked = true;
        }
        
        public IModel getTitle() {
            return new Model("title");
        }

    }

    protected HippoTester tester;
    private Home home;
    protected IPluginContext context;

    @Before
    public void setUp() {
        tester = new HippoTester();
        home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
    }

    @Test
    public void doubleSubmitDoesNotTriggerOnOKTwice() {
        WebRequestCycle cycle = tester.createRequestCycle();
        AjaxRequestTarget target = tester.getApplication().newAjaxRequestTarget(home);
        cycle.setRequestTarget(target);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        dialogService.show(new Dialog());
        
        tester.processRequestCycle(cycle);

        tester.executeAjaxEvent(home.get("dialog:content:form:buttons:0:button"), "onclick");
        tester.executeAjaxEvent(home.get("dialog:content:form:buttons:0:button"), "onclick");
    }

}
