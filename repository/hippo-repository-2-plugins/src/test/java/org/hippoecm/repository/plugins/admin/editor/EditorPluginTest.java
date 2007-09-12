package org.hippoecm.repository.plugins.admin.editor;

import junit.framework.TestCase;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.util.tester.WicketTester;

public class EditorPluginTest extends TestCase {

    private WicketTester tester;

    public void setUp() {
        tester = new WicketTester();
        tester.startPage(EditorPluginTestPage.class);
    }

    public void tearDown() {
        EditorPluginTestPage testPage = (EditorPluginTestPage) tester.getLastRenderedPage();
        testPage.tearDown();
    }

    public void testRenderMyPage() {
        tester.assertRenderedPage(EditorPluginTestPage.class);

        tester.assertComponent("editorPlugin", EditorPlugin.class);
        tester.assertComponent("editorPlugin:editor", NodeEditor.class);
        tester.assertComponent("editorPlugin:editor:properties", PropertiesEditor.class);
        tester.assertComponent("editorPlugin:editor:properties:1:values", PropertyValueEditor.class);
        tester.assertComponent("editorPlugin:editor:properties:1:values:1:value", AjaxEditableLabel.class);
    }

}
