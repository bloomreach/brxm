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

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class AbstractDialogTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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

    public static class FailureDialog extends AbstractDialog {

        @Override
        protected void onOk() {
            error("dialog submit failed");
        }

        public IModel getTitle() {
            return new Model("title");
        }

    }

    protected HippoTester tester;
    private PluginPage home;
    protected IPluginContext context;

    @Before
    public void setUp() {
        tester = new HippoTester();
        home = (PluginPage) tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
    }

    @Test
    public void okButtonIsHiddenAfterSubmit() {
        WebRequestCycle cycle = tester.createRequestCycle();
        AjaxRequestTarget target = tester.getApplication().newAjaxRequestTarget(home);
        cycle.setRequestTarget(target);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        dialogService.show(new Dialog());
        
        tester.processRequestCycle(cycle);

        tester.executeAjaxEvent(home.get("dialog:content:form:buttons:0:button"), "onclick");
        Component button = home.get("dialog:content:form:buttons:0:button");
        assertFalse("OK Button still visible after successful submit", button.isVisibleInHierarchy());
    }

    @Test
    public void okButtonIsPresentAfterFailure() {
        WebRequestCycle cycle = tester.createRequestCycle();
        AjaxRequestTarget target = tester.getApplication().newAjaxRequestTarget(home);
        cycle.setRequestTarget(target);

        IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        dialogService.show(new FailureDialog());

        tester.processRequestCycle(cycle);

        tester.executeAjaxEvent(home.get("dialog:content:form:buttons:0:button"), "onclick");
        Component button = home.get("dialog:content:form:buttons:0:button");
        assertTrue("OK Button was hidden after failed submit", button.isVisibleInHierarchy());
    }
}
