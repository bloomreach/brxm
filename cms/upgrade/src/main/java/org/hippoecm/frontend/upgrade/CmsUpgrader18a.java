/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmsUpgrader18a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(CmsUpgrader18a.class);

    public void register(final UpdaterContext context) {
        context.registerName("cms-upgrade-v18a");
        context.registerStartTag("v16a");
        context.registerEndTag("v18-cms");
        context.registerAfter("repository-upgrade-v18a");

        registerNamespaceVisitors(context);
        registerConsoleVisitors(context);
        registerAuthorizationVisitors(context);
        registerCmsConfigVisitors(context);
        registerEditorVisitors(context);
        registerPublicationWorkflowVisitors(context);
        registerWorkflowVisitors(context);
    }

    private void registerNamespaceVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippotranslation", getClass()
                .getClassLoader().getResourceAsStream("hippotranslation.cnd")));
    }

    private void registerConsoleVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("frontend-console")) {
                    node.getNode("frontend-console").remove();
                }
            }

        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("console")) {
                    node.getNode("console").remove();
                }
            }

        });
    }

    private void registerAuthorizationVisitors(final UpdaterContext context) {
        // hippogallery-imageset
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

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:domains/defaultread") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("reporting-report")) {
                    node.getNode("reporting-report").remove();
                }
            }

        });

        // hippo-workflow
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:domains/workflow") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("frontend-workflow")) {
                    Node domainRule = node.getNode("frontend-workflow");
                    Node facetRule = domainRule.getNode("type-frontend-workflow");
                    facetRule.setProperty("hipposys:value", "hipposys:workflow");
                    context.setName(facetRule, "type-workflow");
                    context.setName(domainRule, "hippo-workflow");
                }
                if (node.hasNode("hippostdpubwf-request")) {
                    node.getNode("hippostdpubwf-request").remove();
                }
            }

        });

    }

    private void registerCmsConfigVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.getNode("cms-folder-views").remove();
                node.getNode("cms-search-views").remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {

            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                for (String view : Arrays.asList("cms-preview", "cms-compare")) {
                    if (node.hasNode(view)) {
                        Node clusterNode = node.getNode(view);
                        if (clusterNode.hasNode("workflowPlugin")) {
                            Node wflPlugin = clusterNode.getNode("workflowPlugin");
                            Value[] categories = wflPlugin.getProperty("workflow.categories").getValues();
                            Value[] newCats = new Value[categories.length + 1];
                            System.arraycopy(categories, 0, newCats, 0, categories.length);
                            newCats[categories.length] = node.getSession().getValueFactory().createValue("translation");
                            wflPlugin.setProperty("workflow.categories", newCats);
                        }
                        if (clusterNode.hasNode("previewPerspective/layout.wireframe")) {
                            Node layout = clusterNode.getNode("previewPerspective/layout.wireframe");
                            layout.setProperty("top", "id=editor-perspective-top,height=25,zindex=1");
                        }
                    }
                }
                if (node.hasNode("cms-editor")) {
                    Node clusterNode = node.getNode("cms-editor");
                    clusterNode.setProperty("frontend:services", new String[] { "wicket.id", "validator.id" });
                    if (clusterNode.hasNode("editPerspective/layout.wireframe")) {
                        Node layout = clusterNode.getNode("editPerspective/layout.wireframe");
                        layout.setProperty("top", "id=editor-perspective-top,height=25,zindex=1");
                    }
                }
                if (node.hasNode("cms-browser/editorManagerPlugin")) {
                    Node editorManager = node.getNode("cms-browser/editorManagerPlugin");
                    context.setName(editorManager.getNode("cluster.compare.options"), "cluster.options");
                    editorManager.getNode("cluster.edit.options").remove();
                    editorManager.getNode("cluster.preview.options").remove();
                }

                // FIXME: folder views are extensible so should be upgraded more carefully
                // TODO: document this limitation
                node.getNode("cms-folder-views").remove();
                node.getNode("cms-search-views").remove();

                {
                    Node documentSection = node.getNode("cms-tree-views/documents");

                    Value[] props = getValues(documentSection, "frontend:properties");
                    Value[] newProps = new Value[props.length + 1];
                    System.arraycopy(props, 0, newProps, 0, props.length);
                    newProps[props.length] = documentSection.getSession().getValueFactory().createValue("nodetypes");
                    documentSection.setProperty("frontend:properties", newProps);

                    Node addRootFolder = documentSection.getNode("addfolderPlugin");
                    addRootFolder.setProperty("workflow.translated", new String[] { "new-translated-folder" });

                    Node treePlugin = documentSection.getNode("documentsBrowser");
                    if (treePlugin.hasNode("module.workflow")) {
                        Value[] categories = getValues(treePlugin.getNode("module.workflow"), "workflow.categories");
                        Value[] newCats = new Value[categories.length + 1];
                        System.arraycopy(categories, 0, newCats, 0, categories.length);
                        newCats[categories.length] = treePlugin.getSession().getValueFactory().createValue(
                                "folder-translations");
                        treePlugin.getNode("module.workflow").setProperty("workflow.categories", newCats);
                    }

                    Node searchingPlugin = documentSection.getNode("sectionPlugin");
                    searchingPlugin.setProperty("nodetypes", "${nodetypes}");
                }

                for (String browser : new String[] { "images/imagesBrowser", "assets/filesBrowser" }) {
                    Node browserPlugin = node.getNode("cms-tree-views/" + browser);
                    Node workflowConfig = browserPlugin.getNode("module.workflow");

                    Value[] categories = getValues(workflowConfig, "workflow.categories");
                    Value[] newCats = new Value[categories.length + 1];
                    System.arraycopy(categories, 0, newCats, 1, categories.length);
                    newCats[0] = workflowConfig.getSession().getValueFactory().createValue("gallery");
                    workflowConfig.setProperty("workflow.categories", newCats);
                }

                Node configTranslations = node.getNode("cms-static/configTranslator/hippostd:translations");
                NodeIterator translationIter = configTranslations.getNodes();
                while (translationIter.hasNext()) {
                    Node translation = translationIter.nextNode();
                    context.setPrimaryNodeType(translation, "frontend:pluginconfig");
                }

                {
                    Node docPicker = node.getNode("cms-pickers/documents");
                    Value[] properties = getValues(docPicker, "frontend:properties");
                    Value[] newProps = new Value[properties.length + 1];
                    System.arraycopy(properties, 0, newProps, 0, properties.length);
                    newProps[properties.length] = docPicker.getSession().getValueFactory().createValue("nodetypes");
                    node.setProperty("frontend:properties", newProps);

                    Node docsLoaderCluster = docPicker.getNode("documentsTreeLoader/cluster.config");
                    docsLoaderCluster.setProperty("nodetypes", "${nodetypes}");
                }
            }

            private Value[] getValues(Node node, String name) throws RepositoryException, ValueFormatException {
                if (node.hasProperty(name)) {
                    Property categoryProp = node.getProperty(name);
                    Value[] categories;
                    if (categoryProp.isMultiple()) {
                        categories = categoryProp.getValues();
                    } else {
                        categories = new Value[] { categoryProp.getValue() };
                    }
                    return categories;
                } else {
                    return new Value[0];
                }
            }
            
        });
    }

    private void registerEditorVisitors(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates/new-type") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                String prototypePath = "hippostd:templates/document/hipposysedit:prototypes/hipposysedit:prototype";
                if (node.hasNode(prototypePath)) {
                    Node prototype = node.getNode(prototypePath);
                    prototype.addMixin("hippotranslation_1_0:translated");
                    prototype.setProperty("hippotranslation_1_0:id", "document-type-locale-id");
                    prototype.setProperty("hippotranslation_1_0:locale", "document-type-locale");
                }
            }
        });
    }

    // FIXME: move to publication-workflow upgrade module?
    private void registerPublicationWorkflowVisitors(final UpdaterContext context) {
        // prevent mixins from being added to document prototype for new namespace
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.addNode("reviewedactions-templateeditor-namespace.xml", "hippo:initializeitem");
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                {
                    Node configurationSection = node.getNode("cms-tree-views/configuration");
                    if (configurationSection.hasNode("configurationBrowser/filters/hideHippostdpubwfNamespace")) {
                        configurationSection.getNode("configurationBrowser/filters/hideHippostdpubwfNamespace")
                                .remove();
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor(
                        "/hippo:configuration/hippo:queries/hippo:templates") {

            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node newDoc = node.getNode("new-document");
                if (newDoc.hasProperty("hippostd:modify")) {
                    Value[] values = newDoc.getProperty("hippostd:modify").getValues();
                    List<Value> newVals = new LinkedList<Value>();
                    for (int i = 0; i < values.length; i += 2) {
                        Value value = values[i];
                        String strVal = value.getString();
                        if (strVal.startsWith("./hippostdpubwf:") || strVal.startsWith("./hippostd:")) {
                            continue;
                        }
                        newVals.add(value);
                        newVals.add(values[i + 1]);
                    }
                    newDoc.setProperty("hippostd:modify", newVals.toArray(new Value[newVals.size()]));
                }
            }
        });
    }

    // FIXME: move to publication-workflow upgrade module?
    private void registerWorkflowVisitors(final UpdaterContext context) {
        // prevent mixins from being added to document prototype for new namespace
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
        });
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:workflows") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                Node threePane = node.getNode("threepane");
                for (String workflow : new String[] {"asset-gallery", "image-gallery"}){
                    Node galleryWorkflow = threePane.getNode(workflow);

                    Set<String> overwritten = new TreeSet<String>();
                    overwritten.add("editor.id");
                    overwritten.add("browser.id");
                    overwritten.add("translator.id");
                    overwritten.add("plugin.class");
                    Node renderer = galleryWorkflow.getNode("frontend:renderer");
                    for (PropertyIterator propIter = renderer.getProperties(); propIter.hasNext();) {
                        Property prop = propIter.nextProperty();
                        if (prop.getDefinition().isProtected() || prop.getName().startsWith("jcr:")
                                || overwritten.contains(prop.getName())) {
                            continue;
                        }
                        prop.remove();
                    }
                    renderer.setProperty("editor.id", "${editor.id}");
                    renderer.setProperty("browser.id", "${browser.id}");
                    renderer.setProperty("translator.id", "service.translator.search");
                    renderer.setProperty("plugin.class", "org.hippoecm.frontend.plugins.standardworkflow.FolderWorkflowPlugin");

                    Node assetWflConfig = galleryWorkflow.getNode("hipposys:config");
                    assetWflConfig.setProperty("modify-on-copy", new String[] { "./hippotranslation:id", "$uuid" });
                }
                for (String workflow : new String[] { "directory", "directory-extended", "folder", "folder-extended" }) {
                    if (threePane.hasNode(workflow + "/frontend:renderer")) {
                        Node wflRenderer = threePane.getNode(workflow + "/frontend:renderer");
                        wflRenderer.setProperty("workflow.translated", new String[] {"new-translated-folder"});
                    }
                }
            }
        });
    }

}
