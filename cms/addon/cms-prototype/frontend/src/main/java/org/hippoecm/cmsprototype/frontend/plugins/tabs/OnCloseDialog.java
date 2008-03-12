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
package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.cmsprototype.frontend.plugins.tabs.TabsPlugin.Tab;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnCloseDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(OnCloseDialog.class);

    private String onclosemessage = "You have possibly changes in this node or child nodes.";

    protected AjaxLink donothing;
    protected AjaxLink discard;
    protected AjaxLink save;
    private TabsPlugin tabsPlugin;
    private Tab tabbie;
    private JcrNodeModel closedJcrNodeModel;
    private ArrayList<JcrNodeModel> jcrNewNodeModelList;
    private Map<JcrNodeModel, Tab> editors;
    /* 
     * meta data keys to store object in session: this is a temporary for because of 
     * HREPTWO-615. When this issue is solved, objects should be correctly serialized
     * and deserialized.
     */
    private static final MetaDataKey DIALOGWINDOW_KEY = new MetaDataKey(DialogWindow.class) {
        private static final long serialVersionUID = 1L;
    };
    private static final MetaDataKey CHANNEL_KEY = new MetaDataKey(Channel.class) {
        private static final long serialVersionUID = 1L;
    };
    private static final MetaDataKey TABSPLUGIN_KEY = new MetaDataKey(TabsPlugin.class) {
        private static final long serialVersionUID = 1L;
    };
    private static final MetaDataKey TAB_KEY = new MetaDataKey(Tab.class) {
        private static final long serialVersionUID = 1L;
    };
    private static final MetaDataKey JCRNODEMODEL_KEY = new MetaDataKey(JcrNodeModel.class) {
        private static final long serialVersionUID = 1L;
    };
    private static final MetaDataKey JCRNODEMODELLIST_KEY = new MetaDataKey(ArrayList.class) {
        private static final long serialVersionUID = 1L;
    };
    private static final MetaDataKey EDITORS_KEY = new MetaDataKey(Map.class) {
        private static final long serialVersionUID = 1L;
    };

    public OnCloseDialog(final DialogWindow dialogWindow, Channel channel, final TabsPlugin tabsPlugin,
            final Tab tabbie, final JcrNodeModel closedJcrNodeModel, final ArrayList<JcrNodeModel> jcrNewNodeModelList,
            Map<JcrNodeModel, Tab> editors) {

        super(dialogWindow, channel);

        this.ok.setVisible(false);
        this.cancel.setVisible(false);

        setSessionMetaDate(dialogWindow, channel, tabsPlugin, tabbie, closedJcrNodeModel, jcrNewNodeModelList, editors);

        setOutputMarkupId(true);

        this.tabsPlugin = tabsPlugin;
        this.tabbie = tabbie;
        this.closedJcrNodeModel = closedJcrNodeModel;
        this.jcrNewNodeModelList = jcrNewNodeModelList;
        this.editors = editors;
        try {
            dialogWindow.setTitle("Close " + closedJcrNodeModel.getNode().getName());
        } catch (RepositoryException e) {
            dialogWindow.setTitle("Close ");
            log.error(e.getMessage());
        }

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
                Result result = save();
                if (result.getRepositoryException() == null) {
                    dialogWindow.close(target);
                } else {
                    // TODO return the exception to the popup, something like below. 
                    //final Label exceptionLabel = new Label("onCloseDialogException" , result.getRepositoryException().getMessage());
                    //target.addComponent(exceptionLabel);
                }
            }
        };
        add(save);

        discard = new AjaxLink("discard") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Result result = discard();
                if (result.getRepositoryException() == null) {
                    dialogWindow.close(target);
                } else {
                    // TODO return the exception to the popup, something like below. 
                    //final Label exceptionLabel = new Label("onCloseDialogException" , result.getRepositoryException().getMessage());
                    //target.addComponent(exceptionLabel);
                }
            }
        };
        add(discard);

        donothing = new AjaxLink("donothing") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                donothing();
                dialogWindow.close(target);
            }
        };
        add(donothing);

        dialogWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                // while HREPTWO-615 we need to fetch objects from the session
                refillInstanceVariables();
                target.addComponent(OnCloseDialog.this.tabsPlugin);
            }
        });

    }

    private void readObject(ObjectInputStream stream) throws IOException {
        // TODO serialization problem wicket
        //System.out.println(this + "readObject");
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        // TODO serialization problem wicket
        //System.out.println(this + "writeObject");
    }

    protected void donothing() {
    }

    protected Result save() {
        // while HREPTWO-615 we need to fetch objects from the session
        refillInstanceVariables();
        Result result = new Result();
        if (closedJcrNodeModel != null) {
            try {
                Node n = closedJcrNodeModel.getNode();

                while (n.isNew()) {
                    n = n.getParent();
                }
                n.save();
                tabbie.destroy();
            } catch (RepositoryException e) {
                result.setRepositoryException(e);
                log.info(e.getClass().getName() + ": " + e.getMessage());
            }
        }
        return result;
    }

    protected Result discard() {
        refillInstanceVariables();
        Result result = new Result();
        if (closedJcrNodeModel != null) {
            try {
                Node n = closedJcrNodeModel.getNode();

                if (n.isNew()) {
                    n.remove();
                } else {
                    String parentPath;
                    parentPath = closedJcrNodeModel.getNode().getPath();
                    for (int i = 0; i < jcrNewNodeModelList.size(); i++) {
                        if (jcrNewNodeModelList.get(i).getNode().getPath().startsWith(parentPath)) {
                            editors.get(jcrNewNodeModelList.get(i)).destroy();
                        }
                    }
                    n.refresh(false);
                }
                tabbie.destroy();

            } catch (RepositoryException e) {
                result.setRepositoryException(e);
                log.info(e.getClass().getName() + ": " + e.getMessage());
            }

        }
        return result;
    }

    @Override
    protected void ok() throws Exception {
    }

    @Override
    protected void cancel() {
    }

    /*
     * TODO below temporary fix while HREPTWO-615
     */

    class Result {
        private RepositoryException repositoryException;

        public RepositoryException getRepositoryException() {
            return repositoryException;
        }

        public void setRepositoryException(RepositoryException repositoryException) {
            this.repositoryException = repositoryException;
        }
    }

    private void setSessionMetaDate(DialogWindow dialogWindow2, Channel channel2, TabsPlugin tabsPlugin2, Tab tabbie2,
            JcrNodeModel closedJcrNodeModel2, ArrayList<JcrNodeModel> jcrNewNodeModelList2,
            Map<JcrNodeModel, Tab> editors2) {

        this.getSession().setMetaData(DIALOGWINDOW_KEY, (Serializable) dialogWindow2);
        this.getSession().setMetaData(CHANNEL_KEY, (Serializable) channel2);
        this.getSession().setMetaData(TABSPLUGIN_KEY, (Serializable) tabsPlugin2);
        this.getSession().setMetaData(TAB_KEY, (Serializable) tabbie2);
        this.getSession().setMetaData(JCRNODEMODEL_KEY, (Serializable) closedJcrNodeModel2);
        this.getSession().setMetaData(JCRNODEMODELLIST_KEY, (Serializable) jcrNewNodeModelList2);
        this.getSession().setMetaData(EDITORS_KEY, (Serializable) editors2);
    }

    private void refillInstanceVariables() {

        Serializable s = this.getSession().getMetaData(DIALOGWINDOW_KEY);
        if (s != null && s instanceof DialogWindow) {
            this.dialogWindow = (DialogWindow) s;
        }
        s = this.getSession().getMetaData(CHANNEL_KEY);
        if (s != null && s instanceof Channel) {
            this.channel = (Channel) s;
        }
        s = this.getSession().getMetaData(TABSPLUGIN_KEY);
        if (s != null && s instanceof TabsPlugin) {
            this.tabsPlugin = (TabsPlugin) s;
        }
        s = this.getSession().getMetaData(TAB_KEY);
        if (s != null && s instanceof Tab) {
            this.tabbie = (Tab) s;
        }
        s = this.getSession().getMetaData(JCRNODEMODEL_KEY);
        if (s != null && s instanceof JcrNodeModel) {
            this.closedJcrNodeModel = (JcrNodeModel) s;
        }
        s = this.getSession().getMetaData(JCRNODEMODELLIST_KEY);
        if (s != null && s instanceof ArrayList) {
            this.jcrNewNodeModelList = (ArrayList<JcrNodeModel>) s;
        }
        s = this.getSession().getMetaData(EDITORS_KEY);
        if (s != null && s instanceof Map) {
            this.editors = (Map<JcrNodeModel, Tab>) s;
        }
    }
    /*
     * end temporary fix while HREPTWO-615 
     */
}
