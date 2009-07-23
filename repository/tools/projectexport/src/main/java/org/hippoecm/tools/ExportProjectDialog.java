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
package org.hippoecm.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportProjectDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ExportProjectDialog.class);

    private String projectName;
    private String location;
    private Label projectNameComponent;
    private ExportProjectTree treeComponent;

    public ExportProjectDialog() {
        this.setOkVisible(false);
        this.setCancelVisible(false);

        Form form;
        add(form = new Form("form"));
        form.add(new RequiredTextField("input", new PropertyModel(this, "location")));
        form.add(new AjaxButton("ok", form) {
            public void onSubmit(AjaxRequestTarget target, Form form) {
                if(projectName == null || projectName.equals(""))
                    return;
                try {
                    System.err.println("BERRY selected project "+projectName);
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
        form.add(new DownloadLink("download", new Model("download")) {
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
                                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                                ex.printStackTrace(System.err);
                            }
                        }
                    };
                    thread.start();
                    return pstream;
                } catch(RepositoryException ex) {
                                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                                ex.printStackTrace(System.err);
                    log.error("failed to export project ", ex);
                    return null;
                } catch(IOException ex) {
                                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                                ex.printStackTrace(System.err);
                    log.error("failed to export project ", ex);
                    return null;
                } catch(NotExportableException ex) {
                                System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                                ex.printStackTrace(System.err);
                    log.error("failed to export project ", ex);
                    return null;
                }
            }
        });
        add(projectNameComponent = new Label("name", new PropertyModel(this, "projectName")));
        projectNameComponent.setOutputMarkupId(true);
        add(treeComponent = new ExportProjectTree("tree", new ExportTreeModel(), new Component[] { projectNameComponent } ));
        treeComponent.setRootLess(true);
        treeComponent.getTreeState().setAllowSelectMultiple(false);
        treeComponent.getTreeState().addTreeStateListener(new ITreeStateListener() {
            public void nodeSelected(TreeNode node) {
                TreeNode ancestor = ((ExportTreeModel)treeComponent.getModelObject()).backingTreeNode(node);
                if(ancestor != null && ancestor != node) {
                    treeComponent.getTreeState().selectNode(ancestor, true);
                    projectName = ancestor.toString();
                    projectNameComponent.setModel(new Model(projectName));
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
