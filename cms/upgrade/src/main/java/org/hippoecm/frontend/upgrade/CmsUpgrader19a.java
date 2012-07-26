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
package org.hippoecm.frontend.upgrade;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmsUpgrader19a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(CmsUpgrader19a.class);

    public void register(final UpdaterContext context) {
        context.registerName("cms-upgrade-v19a");
        context.registerStartTag("v18b-cms");
        context.registerEndTag("v19-cms");
        context.registerAfter("repository-upgrade-v19a");

        registerAuthorizationVisitors(context);
        registerQueriesVisitors(context);
        registerWorkflowVisitors(context);
        registerNamespaceVisitors(context);
        registerCmsConfigVisitors(context);
        registerLoginConfigVisitors(context);
        registerTranslationUpdates(context);
    }

    private void registerAuthorizationVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("domain-hippogallery")) {
                    node.getNode("domain-hippogallery").remove();
                }
            }

        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:domains/hippogallery") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }

        });

    }

    private void registerQueriesVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(final Node node, final int level) throws RepositoryException {
                node.getNode("templateeditor-namespace.xml").remove();
                node.getNode("templateeditor-namespace_fr").remove();
                node.getNode("templateeditor-type-query.xml").remove();
                node.getNode("templateeditor-type-query-fr.xml").remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("Template Editor Namespace")) {
                    node.getNode("Template Editor Namespace").remove();
                }
                if (node.hasNode("new-type")) {
                    node.getNode("new-type").remove();
                }
                // some problem with the updater nodes prevents the hipposysedit:uri property from being set.
                // Don't wanna know, don't care right now.
                /*if (node.hasNode("Template Editor Namespace")) {
                    Node nsTemplate = node.getNode("Template Editor Namespace");
                    nsTemplate.setProperty("hippostd:modify", new String[]{"./_name", "$name",
                            "./basedocument/hipposysedit:nodetype/hipposysedit:nodetype/hipposysedit:uri", "$uri"});
                    Node ntNode = nsTemplate.getNode("hippostd:templates/namespace/basedocument/hipposysedit:nodetype/hipposysedit:nodetype");
                    ntNode.setProperty("hipposysedit:uri", "uri");
                }
                if (node.hasNode("new-type")) {
                    Node newTypeNode = node.getNode("new-type");
                    newTypeNode.setProperty("hippostd:modify", new String[]{"./_name", "$name", "./hipposysedit:nodetype/hipposysedit:nodetype/hipposysedit:supertype[0]", "$supertype"});

                    Node compoundNtNode = newTypeNode.getNode("hippostd:templates/compound/hipposysedit:nodetype/hipposysedit:nodetype");
                    compoundNtNode.setProperty("hipposysedit:supertype", new String[]{ "hippo:compound", "hippostd:relaxed" });

                    Node documentNtNode = newTypeNode.getNode("hippostd:templates/document/hipposysedit:nodetype/hipposysedit:nodetype");
                    documentNtNode.setProperty("hipposysedit:supertype", new String[]{ "hippo:document", "hippostd:relaxed" });
                }*/
            }

        });
    }

    private void registerWorkflowVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:workflows/default") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("default/frontend:renderer")) {
                    Node renderer = node.getNode("default/frontend:renderer");
                    renderer.setProperty("plugin.class", "org.hippoecm.frontend.editor.workflow.DefaultWorkflowPlugin");
                }
            }

        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:workflows/editing") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("default/frontend:renderer")) {
                    Node renderer = node.getNode("default/frontend:renderer");
                    renderer.setProperty("plugin.class", "org.hippoecm.frontend.editor.workflow.EditingDefaultWorkflowPlugin");
                }
            }

        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:workflows/editor") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("template-editor-namespace/frontend:renderer")) {
                    Node renderer = node.getNode("template-editor-namespace/frontend:renderer");
                    renderer.setProperty("plugin.class", "org.hippoecm.frontend.editor.workflow.NamespaceWorkflowPlugin");
                }
                if (node.hasNode("template-editor-namespaces/frontend:renderer")) {
                    Node renderer = node.getNode("template-editor-namespaces/frontend:renderer");
                    renderer.setProperty("plugin.class", "org.hippoecm.frontend.editor.workflow.TemplateEditorWorkflowPlugin");
                }
            }

        });
        /*context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:workflows/goto") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }

        });*/
    }

    private void registerNamespaceVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (String initName : new String[]{"templateeditor-system", "templateeditor-system_fr", "templateeditor-hippo", "templateeditor-hippo_fr", "templateeditor-hipposysedit", "templateeditor-hipposysedit_fr", "hippostd-types", "hippostd-types-fr",}) {
                    if (node.hasNode(initName)) {
                        node.getNode(initName).remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (String prefix : new String[]{"system", "hippo", "hippostd", "hipposysedit"}) {
                    if (node.hasNode(prefix)) {
                        node.getNode(prefix).remove();
                    }
                }
            }

        });
    }

    private void registerCmsConfigVisitors(final UpdaterContext context) {

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("cms-browser/navigator")) {
                    node.getNode("cms-browser/navigator").setProperty("model.folder", "model.browse.folder");
                }
                if (node.hasNode("cms-compare/workflowPlugin")) {
                    removeGotoCategory(node.getNode("cms-compare/workflowPlugin"));
                }
                if (node.hasNode("cms-preview/workflowPlugin")) {
                    removeGotoCategory(node.getNode("cms-preview/workflowPlugin"));
                }
                if (node.hasNode("cms-dashshortcuts/newEventShortcut")) {
                    node.getNode("cms-dashshortcuts/newEventShortcut").setProperty("query", "new-event-viawizard");
                }
                if (node.hasNode("cms-dashshortcuts/newNewsItemShortcut")) {
                    node.getNode("cms-dashshortcuts/newNewsItemShortcut").setProperty("query", "new-news-viawizard");
                }
                if (node.hasNode("cms-static/adminPerspective")) {
                    Node perspective = node.getNode("cms-static/adminPerspective");
                    perspective.remove();
                    Node loader = node.getNode("cms-static").addNode("adminLoader", "frontend:plugin");
                    loader.setProperty("cluster.name", "cms-admin");
                    loader.setProperty("plugin.class", "org.hippoecm.frontend.plugin.loader.PluginClusterLoader");
                }
            }

            private void removeGotoCategory(final Node node) throws RepositoryException {
                if (node.hasProperty("workflow.categories")) {
                    Value[] categories = node.getProperty("workflow.categories").getValues();
                    Value[] newCats = categories;
                    for (int i = 0; i < categories.length; i++) {
                        if ("goto".equals(categories[i].getString())) {
                            newCats = new Value[categories.length - 1];
                            System.arraycopy(categories, 0, newCats, 0, i);
                            System.arraycopy(categories, i + 1, newCats, i, newCats.length - i);
                            break;
                        }
                    }
                    categories = newCats;
                    for (int i = 0; i < categories.length; i++) {
                        if ("custom".equals(categories[i].getString())) {
                            newCats = new Value[categories.length + 1];
                            System.arraycopy(categories, 0, newCats, 0, i);
                            newCats[i] = node.getSession().getValueFactory().createValue("committype");
                            System.arraycopy(categories, i, newCats, i + 1, categories.length - i);
                            break;
                        }
                    }
                    node.setProperty("workflow.categories", newCats);
                }
            }
        });



        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.getNode("cms-folder-views").remove();
                node.getNode("cms-search-views").remove();
            }
        });
    }

    private void registerLoginConfigVisitors(final UpdaterContext context) {

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/login") {
            @Override
            protected void leaving(final Node node, final int level) throws RepositoryException {
                node.remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize/cms-login") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }
        });
    }

    void registerTranslationUpdates(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (String folderType : new String[]{"new-file-folder", "new-image-folder"}) {
                    Node newTranslatedFolder = node.getNode(folderType);
                    for (NodeIterator iter = newTranslatedFolder.getNodes("hippo:translation"); iter.hasNext();) {
                        Node txn = iter.nextNode();
                        if ("fr".equals(txn.getProperty("hippo:language").getString())) {
                            txn.setProperty("hippo:message", "un nouveau dossier");
                        }
                    }
                }
                Node newTranslatedFolder = node.getNode("new-translated-folder");
                for (NodeIterator iter = newTranslatedFolder.getNodes("hippo:translation"); iter.hasNext();) {
                    Node txn = iter.nextNode();
                    if ("fr".equals(txn.getProperty("hippo:language").getString())) {
                        txn.setProperty("hippo:message", "un nouveau dossier traduit");
                    }
                }
            }
        });
    }
}
