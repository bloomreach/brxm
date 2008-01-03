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
package org.hippoecm.frontend.plugins.admin.editor;

import junit.framework.TestCase;

import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class EditorPluginTest extends TestCase {

    private WicketTester tester;
    private Application application;

    @Override
    public void setUp() {
        application = new Application();
        application.setUp();

        tester = new WicketTester(application);
        tester.startPage(EditorPluginTestPage.class);
    }

    @Override
    public void tearDown() {
        application.tearDown();
    }

    public void testRenderMyPage() {
        tester.assertRenderedPage(EditorPluginTestPage.class);

        tester.assertComponent("editorPlugin", EditorPlugin.class);
        tester.assertComponent("editorPlugin:editor", NodeEditor.class);
        tester.assertComponent("editorPlugin:editor:properties", PropertiesEditor.class);
        tester.assertComponent("editorPlugin:editor:properties:1:values", PropertyValueEditor.class);
        tester.assertComponent("editorPlugin:editor:properties:1:values:1:value", TextFieldWidget.class);

        tester.assertComponent("editorPlugin:editor:types", NodeTypesEditor.class);
        tester.assertComponent("editorPlugin:editor:types:type:0:check", Check.class);
    }

}
