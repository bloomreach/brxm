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
package org.hippoecm.repository.frontend.wysiwyg.xinha;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

import org.hippoecm.repository.frontend.wysiwyg.HtmlEditor;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

public class XinhaEditor extends Panel {
    private static final long serialVersionUID = 1L;

    private String content;

    /*private static final ResourceReference JS = new ResourceReference(
            XinhaEditor.class, "impl/xinha/XinhaCore.js");*/

    private TextArea editor;
    private XinhaEditorConf editorConf;

    private final AbstractDefaultAjaxBehavior postBehaviour;
    private XinhaEditorConfigurationBehaviour bh;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public XinhaEditor(final String id, XinhaEditorConfigurationBehaviour bh) {
        super(id);
        setModel(new PropertyModel(this, "content"));

        this.bh = bh;

        editor = new TextArea("editor", getModel());
        editor.setOutputMarkupId(true);
        editor.setVisible(true);

        postBehaviour = new AbstractDefaultAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            protected void respond(AjaxRequestTarget target) {
                RequestCycle requestCycle = RequestCycle.get();
                boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save")).booleanValue();

                if (save) {
                    editor.processInput();

                    System.out.println("editor value: " + editor.getValue());
                    System.out.println("editor contents: " + XinhaEditor.this.getContent());

                    if (editor.isValid()) {
                        System.out.println("ALLES GOED");
                    }
                }
            }
        };

        editor.add(postBehaviour);

        editorConf = new XinhaEditorConf();
        editorConf.setName("editor2"); // FIXME ??? conf.setName(editor.getMarkupId());
        editorConf.setPlugins(new String[] { "WicketSave", "CharacterMap", "ContextMenu", "ListType", "SpellChecker",
                "Stylist", "SuperClean", "TableOperations" });

        add(editor);
    }

    XinhaEditorConf getConfiguration() {
        return editorConf;
    }

    public void init() {
        Map conf = new Hashtable();
        conf.put("postUrl", postBehaviour.getCallbackUrl());
        editorConf.setConfiguration(conf);

        bh.addConfiguration(editorConf);
    }
}
