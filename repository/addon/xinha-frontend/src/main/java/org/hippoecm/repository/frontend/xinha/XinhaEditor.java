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
package org.hippoecm.repository.frontend.xinha;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

import org.apache.wicket.markup.ComponentTag;

import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.widgets.AjaxUpdatingWidget;

public class XinhaEditor extends AjaxUpdatingWidget /*Panel*/
    implements IHeaderContributor
{
    private static final long serialVersionUID = 1L;

    private String content;

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

    public XinhaEditor(final String id, JcrPropertyValueModel model) {
        super(id, model);

        editor = new TextArea("value", getModel());

        editor.setOutputMarkupId(true);
        editor.setVisible(true);
        // setRenderBodyOnly(true);

        postBehaviour = new AbstractDefaultAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    final String saveCall = "{wicketAjaxGet('" + getCallbackUrl()
                        + "&save=true&'+this.name+'='+wicketEncode(this.value)); return false;}";
                    tag.put("onblur", saveCall);
                }

                protected void respond(AjaxRequestTarget target) {
                    RequestCycle requestCycle = RequestCycle.get();
                    boolean save = Boolean.valueOf(requestCycle.getRequest().getParameter("save")).booleanValue();
                    if (save) {
                        editor.processInput();
                    }
                }
            };

        editor.add(postBehaviour);

        add(editor);

        editorConf = new XinhaEditorConf();
    }

    public void onBeforeRender() {
        if (bh == null) {
            Page page = findPage();
            List bhs = page.getBehaviors();
            for(Iterator iter = bhs.iterator(); iter.hasNext(); ) {
                IBehavior behavior = (IBehavior) iter.next();
                if(behavior instanceof XinhaEditorConfigurationBehaviour) {
                    bh = (XinhaEditorConfigurationBehaviour) behavior;
                    break;
            }
            }
            if(bh == null) {
                bh = new XinhaEditorConfigurationBehaviour();
                // page.add(bh);
            }
        }

        editorConf.setName(editor.getMarkupId());
        editorConf.setPlugins(new String[] { "CharacterMap", "ContextMenu", "ListType", "SpellChecker",
                                             "Stylist", "SuperClean", "TableOperations" }); // "WicketSave", 
        Map conf = new Hashtable();
        conf.put("postUrl", postBehaviour.getCallbackUrl());
        editorConf.setConfiguration(conf);
        bh.addConfiguration(editorConf);

        super.onBeforeRender();
    }

    public void init() {
    }

    public void renderHead(IHeaderResponse response)
    {
        IHeaderContributor[] contribs = bh.getHeaderContributors();
        for(int i=0; i<contribs.length; i++)
            contribs[i].renderHead(response);
    }
}
