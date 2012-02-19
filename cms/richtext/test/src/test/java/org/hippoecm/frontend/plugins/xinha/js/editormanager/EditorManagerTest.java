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
package org.hippoecm.frontend.plugins.xinha.js.editormanager;

import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInlineFrame;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.xinha.XinhaPage;
import org.hippoecm.frontend.plugins.xinha.XinhaTest;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore // YUI 2.9.0 uses getBoundingRectClient, which is not supported by htmlunit
public class EditorManagerTest extends XinhaTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static String value;

    public static class Page extends XinhaPage {

        public Page() {
            setOutputMarkupId(true);

            final Label label = new Label("label", new Model("unclicked")) {

                @Override
                public String getMarkupId() {
                    return "label3";
                }
            };
            label.setOutputMarkupId(true);
            AjaxLink link = new AjaxLink("link") {
                private static final long serialVersionUID = 1L;

                @Override
                public String getMarkupId() {
                    return "link2";
                }

                @Override
                public void onClick(AjaxRequestTarget target) {
                    label.setDefaultModel(new Model("clicked"));
                    target.addComponent(label);
                }
                
            };
            link.add(label);
            add(link);

            final EditorManagerBehavior amb = new EditorManagerBehavior();
            final MarkupContainer container = new WebMarkupContainer("container");
            container.setOutputMarkupId(true);
            container.add(new AjaxEventBehavior("onclick") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    amb.setEditorStarted(true);
                    amb.setFocusAfterLoad(true);
                    target.addComponent(container);
                }
            });
            TextArea<String> tif = new TextArea<String>("xinha", new IModel<String>() {

                public String getObject() {
                    return EditorManagerTest.value;
                }

                public void setObject(String object) {
                    EditorManagerTest.value = object;
                }

                public void detach() {
                }
            }) {

                @Override
                public String getMarkupId() {
                    return "xinha1";
                }
            };
            tif.setOutputMarkupId(true);
            tif.add(amb);
            tif.add(new AutoSaveBehavior());
            container.add(tif);
            add(container);
        }
    }

    @Test
    public void testAutoSaveProduction() throws Exception {
        setDeployed(true);
        testAutoSave();
    }

    @Test
    public void testAutoSaveDevelopment() throws Exception {
        setDeployed(false);
        testAutoSave();
    }

    /**
     * - open a textarea
     * - click to edit
     * - set text
     * - click link outside text
     * =&gt; text should have been saved
     * @throws Exception 
     */
    public void testAutoSave() throws Exception {
        setUp(Page.class);

        value = null;

        page.getElementById("xinha1").click();

        int nretries = 0;
        List<HtmlElement> elements = null;
        do {
            assertTrue(nretries++ < 20);
            Thread.sleep(500);
            elements = page.getElementsByTagName("iframe");
        } while (elements.size() < 2);

        // HTMLUNIT doesn't fire onload: issue 2955073
        HtmlInlineFrame iframe = (HtmlInlineFrame) elements.get(1);
        iframe.fireEvent("load");

        HtmlPage enclosed = (HtmlPage) iframe.getEnclosedPage();
        enclosed.getElementsByTagName("body").get(0).setTextContent("aap noot mies");

        page.getElementById("link2").click();

        nretries = 0;
        do {
            assertTrue(nretries++ < 20);
            Thread.sleep(300);
        } while ("unclicked".equals(page.getElementById("label3").getTextContent()));

        assertEquals("aap noot mies", value);
    }

}
