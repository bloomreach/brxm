/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools.projectexport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.dialog.AbstractDialog;

public class ExportDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ExportDialog.class);

    private String projectName;
    private String location;
    private Label projectNameComponent;
    private Button okComponent;
    private ExportTree treeComponent;
    private Label statusComponent;

    public ExportDialog() {
        this.setOutputMarkupId(true);
        this.setOkVisible(false);
        this.setCancelVisible(false);
        add(statusComponent = new Label("status", new Model("Select a project to be exported and enter the server side file system location of the subversion project to update.  Be sure to only use this feature using administrative rights.  Check the log file for errors afterwards!")));
        statusComponent.setOutputMarkupId(true);
        add(new RequiredTextField("input", new PropertyModel(this, "location")));
        add(okComponent = new AjaxButton("ok", this) {
            public void onSubmit(AjaxRequestTarget target, Form form) {
                okComponent.setEnabled(false);
                ExportDialog.this.addOrReplace(feedback = new FeedbackPanel("tree"));
                statusComponent.setModel(new Model("Exported; check log files in case of errors"));
                target.addComponent(ExportDialog.this);
                if(projectName == null || projectName.equals(""))
                    return;
                try {
                    ((ExportTreeModel)treeComponent.getModelObject()).getExporter().selectProject(projectName);
                    File basedir = new File(location);
                    if(basedir.exists()) {
                        if(!basedir.isDirectory()) {
                            throw new IOException("invalid base directory");
                        }
                        if(!new File(basedir, "pom.xml").exists()) {
                            throw new IOException("invalid project structure");
                        }
                    } else {
                        if(!basedir.getParentFile().isDirectory()) {
                            throw new IOException("path does not exist");
                        }
                        basedir.mkdir();
                    }
                    ((ExportTreeModel)treeComponent.getModelObject()).getExporter().exportProject(basedir);
                } catch(RepositoryException ex) {
                    log.error("failed to export project ", ex);
                } catch(IOException ex) {
                    log.error("failed to export project ", ex);
                } catch(NotExportableException ex) {
                    log.error("failed to export project ", ex);
                }
            }
        });
        add(new DownloadLink("download", new Model("download")) {
            protected String getFilename() {
                return projectName + ".zip";
            }
            protected InputStream getContent() {
                if(projectName == null || projectName.equals(""))
                    return null;
                try {
                    ((ExportTreeModel)treeComponent.getModelObject()).getExporter().selectProject(projectName);
                    final PipedOutputStream ostream = new PipedOutputStream();
                    final PipedInputStream pstream = new PipedInputStream(ostream);
                    Thread thread = new Thread() {
                        public void run() {
                            try {
                                try {
                                    ((ExportTreeModel)treeComponent.getModelObject()).getExporter().exportProject(ostream);
                                } catch (RepositoryException ex) {
                                    pstream.close();
                                } catch (IOException ex) {
                                    pstream.close();
                                } catch (NotExportableException ex) {
                                    pstream.close();
                                }
                            } catch (IOException ex) {
                                log.error("failed to export project ", ex);
                            }
                        }
                    };
                    thread.start();
                    return pstream;
                } catch(RepositoryException ex) {
                    log.error("failed to export project ", ex);
                    return null;
                } catch(IOException ex) {
                    log.error("failed to export project ", ex);
                    return null;
                } catch(NotExportableException ex) {
                    log.error("failed to export project ", ex);
                    return null;
                }
            }
        });
        okComponent.setEnabled(false);
        okComponent.setOutputMarkupId(true);
        add(projectNameComponent = new Label("name", new PropertyModel(this, "projectName")));
        projectNameComponent.setOutputMarkupId(true);
        add(new AjaxLink("tree", new Model("click here to activate")) {
            public void onClick(AjaxRequestTarget target) {
                loadtree(target);
                okComponent.setEnabled(true);
                target.addComponent(okComponent);
                target.addComponent(ExportDialog.this);
            }
        });
    }
    
    private void loadtree(AjaxRequestTarget target) {
        addOrReplace(treeComponent = new ExportTree("tree", new ExportTreeModel(), new Component[] { projectNameComponent } ));
        treeComponent.setRootLess(true);
        treeComponent.getTreeState().setAllowSelectMultiple(false);
        treeComponent.getTreeState().addTreeStateListener(new ITreeStateListener() {
            public void nodeSelected(TreeNode node) {
                ExportTreeModel treeModel = (ExportTreeModel) treeComponent.getModelObject();
                TreeNode ancestor = treeModel.backingTreeNode(node);
                projectName = ((Element.ProjectElement)treeModel.backingElement(ancestor)).projectName;
                projectNameComponent.setModel(new Model(projectName));
                if(ancestor != null && ancestor != node) {
                    treeComponent.getTreeState().selectNode(ancestor, true);
                }
            }
            public void nodeUnselected(TreeNode node) {
            }
            public void nodeExpanded(TreeNode node) {
            }
            public void nodeCollapsed(TreeNode node) {
            }
            public void allNodesCollapsed() {
            }       
            public void allNodesExpanded() {
            }
        });
    }

    public IModel getTitle() {
        return new Model("Export Project");
    }
}
