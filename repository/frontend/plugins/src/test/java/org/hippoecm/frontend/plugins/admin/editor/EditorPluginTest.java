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
