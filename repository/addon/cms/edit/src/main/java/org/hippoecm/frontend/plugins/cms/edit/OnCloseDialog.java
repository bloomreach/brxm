/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.edit;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnCloseDialog extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(OnCloseDialog.class);

    private String onclosemessage = "You have possibly changes in this node or child nodes.";

    protected AjaxLink donothing;
    protected AjaxLink discard;
    protected AjaxLink save;
    private JcrNodeModel model;
    private IServiceReference<IEditorManager> factory;
    private IServiceReference<IEditService> editor;

    public OnCloseDialog(IPluginContext context, IDialogService dialogWindow, JcrNodeModel model, IEditorManager mgr, IEditService editor) {
        super(dialogWindow);

        this.model = model;
        this.factory = context.getReference(mgr);
        this.editor = context.getReference(editor);

        this.ok.setVisible(false);
        this.cancel.setVisible(false);

        setOutputMarkupId(true);

        final Label onCloseMessageLabel = new Label("onclosemessage", onclosemessage);
        onCloseMessageLabel.setOutputMarkupId(true);
        add(onCloseMessageLabel);

        final Label exceptionLabel = new Label("onCloseDialogException", "");
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        save = new AjaxLink("save") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                save();
                IEditorManager plugin = factory.getService();
                plugin.deleteEditor(OnCloseDialog.this.editor.getService());
                closeDialog();
            }
        };
        add(save);

        discard = new AjaxLink("discard") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                discard();
                IEditorManager plugin = factory.getService();
                plugin.deleteEditor(OnCloseDialog.this.editor.getService());
                closeDialog();
            }
        };
        add(discard);

        donothing = new AjaxLink("donothing") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                donothing();
                closeDialog();
            }
        };
        add(donothing);
    }

    protected void save() {
        try {
            Node n = model.getNode();

            while (n.isNew()) {
                n = n.getParent();
            }
            n.save();
        } catch (RepositoryException e) {
            log.info(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected void discard() {
        try {
            Node n = model.getNode();

            if (n.isNew()) {
                n.remove();
            } else {
                n.refresh(false);
            }
        } catch (RepositoryException e) {
            log.info(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    protected void donothing() {
    }

    public IModel getTitle() {
        return new StringResourceModel("close-document", this, null, new Object[] {
                new PropertyModel(model, "node.name")
        }, "Close {0}");
    }

}
