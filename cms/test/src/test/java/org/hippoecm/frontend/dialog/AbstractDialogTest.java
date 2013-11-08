/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

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
        
        public IModel<String> getTitle() {
            return Model.of("title");
        }

    }

    public static class FailureDialog extends AbstractDialog {

        @Override
        protected void onOk() {
            error("dialog submit failed");
        }

        public IModel<String> getTitle() {
            return Model.of("title");
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
    public void dialogIsClosedAfterSuccessfulSubmit() {
        tester.runInAjax(home, new Runnable() {

            @Override
            public void run() {
                IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                dialogService.show(new Dialog());
            }

        });

        tester.executeAjaxEvent(home.get("dialog:content:form:buttons:0:button"), "onclick");
        MarkupContainer content = (MarkupContainer) home.get("dialog:content");
        assertEquals(0, content.size());
    }

    @Test
    public void okButtonIsPresentAfterFailure() {
        tester.runInAjax(home, new Runnable() {

            @Override
            public void run() {
                IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                dialogService.show(new FailureDialog());
            }
        });

        tester.executeAjaxEvent(home.get("dialog:content:form:buttons:0:button"), "onclick");
        Component button = home.get("dialog:content:form:buttons:0:button");
        assertTrue("OK Button was hidden after failed submit", button.isVisibleInHierarchy());
    }
}
