/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.upgrade;

import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader12a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(Upgrader12a.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v12a");
        context.registerStartTag("v20902");
        context.registerEndTag("v12b");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                /*
                 * The removal of the entire /hippo:log tree seems to be appropriate.  This is relatively volatile data as
                 * this is a sliding log file with the oldest entries being removed automatically.  Combine this with the
                 * fact that old entries might not contain the same information and the effort of converting data which is
                 * going to be removed quickly is unnecessary.
                 */
                if (node.hasNode("hippo:log")) {
                    for(NodeIterator iter=node.getNode("hippo:log").getNodes(); iter.hasNext(); ) {
                        iter.nextNode().remove();
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostd:html") {
            Pattern pattern = Pattern.compile("(<img[^>]*src=\")([^/]*)/([^/]*)/([^\"]*\")", Pattern.MULTILINE);
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                Property htmlProperty = node.getProperty("hippostd:content");
                Matcher matcher = pattern.matcher(htmlProperty.getString());
                StringBuffer sb = new StringBuffer();
                while(matcher.find()) {
                    matcher.appendReplacement(sb, "$1$2/{_document}/$4");
                }
                matcher.appendTail(sb);
                htmlProperty.setValue(new String(sb));
              }
        });
        for(String path : new String[] {
            "/hippo:configuration/hippo:queries/hippo:templates/Template Editor Namespace",
            "/hippo:configuration/hippo:queries/hippo:templates/new-type",
            "/hippo:configuration/hippo:initialize/templateeditor-namespace.xml",
            "/hippo:configuration/hippo:initialize/templateeditor-type-query.xml"
        }) {
            context.registerVisitor(new UpdaterItemVisitor.PathVisitor(path) {
                @Override
                public void leaving(final Node node, int level) throws RepositoryException {
                    node.remove();
                }
            });
        }
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hipposysedit:nodetype") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                context.setName(node, "hipposysedit_1_1:nodetype");
                context.setPrimaryNodeType(node, "hipposysedit_1_1:nodetype");
                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                    Node field = iter.nextNode();
                    context.setPrimaryNodeType(field, "hipposysedit_1_1:field");
                    if(field.hasProperty("hipposysedit_1_1:name")) {
                        Property nameProperty = field.getProperty("hipposysedit_1_1:name");
                        if(field.getName().equals("hipposysedit:field")) {
                            context.setName(field, nameProperty.getString());
                        }
                        nameProperty.remove();
                    }
                    if(field.hasProperty("hipposysedit_1_1:mandatory")) {
                        field.getProperty("hipposysedit_1_1:mandatory").remove();
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostdpubwf", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippostdpubwf.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposysedit", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposysedit_1_1.cnd"))));

        // re-read parts of the configuration
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.getNode("frontend-console").remove();
                node.getNode("cms-preview").remove();
                node.getNode("cms-editor").remove();
                node.getNode("cms-pickers").remove();
                node.getNode("cms-tree-views").remove();

                for (String initName : new String[] { "templateeditor-hippo", "hippostd-types",
                        "hippogallery-editor", "templateeditor-faceteddate", "hippostd-html-template",
                        "templateeditor-hipposysedit" }) {
                    for (NodeIterator nodes = node.getNodes(initName); nodes.hasNext(); ) {
                        nodes.nextNode().remove();
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hipposys:type") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
               if (node.hasProperty("hipposys:nodetype")) {
                   if ("hipposys:request".equals(node.getProperty("hipposys:nodetype").getString())) {
                       node.setProperty("hipposys:nodetype", "hippostdpubwf:request");
                   } else if ("hippostd:publishable".equals(node.getProperty("hipposys:nodetype").getString())) {
                       node.setProperty("hipposys:nodetype", "hippostdpubwf:document");
                   }
               }
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:roles/admin") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                   node.setProperty("hipposys:privileges", new String[] {"jcr:all", "hippo:admin"});
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates/simple/hippostd:templates/new-document/new-document") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.setProperty("hippostd:state", "draft");
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates/new-type/hippostd:templates/document/hipposysedit:prototypes/hipposysedit:prototype") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.setProperty("hippostd:holder", "$holder");
                node.setProperty("hippostd:state", "draft");
                node.setProperty("hippostdpubwf:createdBy", "");
                node.setProperty("hippostdpubwf:creationDate", "2008-03-26T12:03:00.000+01:00");
                node.setProperty("hippostdpubwf:lastModificationDate", "2008-03-26T12:03:00.000+01:00");
                node.setProperty("hippostdpubwf:lastModifiedBy", "");
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates/new-document") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.setProperty("hippostd:modify", new String[] {
                        "./_name", "$name",
                        "./hippostdpubwf:createdBy", "$holder",
                        "./hippostdpubwf:creationDate", "$now",
                        "./hippostdpubwf:lastModifiedBy", "$holder",
                        "./hippostdpubwf:lastModificationDate", "$now",
                        "./hippostd:holder", "$holder"
                    });
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/console") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }
        });

        // derivatives
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:derivatives") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if(node.hasNode("reviewed-action/hipposys:accessed/request")) {
                    Node reviewedAction = node.getNode("reviewed-action/hipposys:accessed/request");
                    reviewedAction.setProperty("hipposys:relPath", "../request[@hippostdpubwf:type='publish']/type");
                }
            }
        });

        // hide prototypes from search
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:domains") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node hippoDocument = node.getNode("hippodocuments/hippo-document");

                Node hideProtos = hippoDocument.addNode("hide-prototypes", "hipposys:facetrule");
                hideProtos.setProperty("hipposys:equals", false);
                hideProtos.setProperty("hipposys:facet", "nodename");
                hideProtos.setProperty("hipposys:filter", false);
                hideProtos.setProperty("hipposys:type", "Name");
                hideProtos.setProperty("hipposys:value", "hipposysedit:prototype");

                Node request = node.getNode("hipporequests/hippo-request/nodetype-hippo-request");
                request.setProperty("hipposys:value", "hippo:request");

                Node workflowReq = node.getNode("workflow/hippo-request");
                workflowReq.getNode("type-hippo-request").setProperty("hipposys:value", "hippo:request");

                Node pubwfReq = node.getNode("workflow").addNode("hippostdpubwf-request", "hipposys:domainrule");
                Node pubwfReqType = pubwfReq.addNode("type-hippostdpubwf-request", "hipposys:facetrule");
                pubwfReqType.setProperty("hipposys:equals", true);
                pubwfReqType.setProperty("hipposys:facet", "jcr:primaryType");
                pubwfReqType.setProperty("hipposys:filter", false);
                pubwfReqType.setProperty("hipposys:type", "Name");
                pubwfReqType.setProperty("hipposys:value", "hippostdpubwf:request");

                Node readwrite = node.getNode("defaultwrite");
                Node translation = readwrite.addNode("hippo-translation", "hipposys:domainrule");
                Node translationType = translation.addNode("type-hippo-translation", "hipposys:facetrule");
                translationType.setProperty("hipposys:equals", true);
                translationType.setProperty("hipposys:facet", "jcr:primaryType");
                translationType.setProperty("hipposys:filter", false);
                translationType.setProperty("hipposys:type", "Name");
                translationType.setProperty("hipposys:value", "hippo:translation");

            }
        });

        // cms
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {

                if(node.hasNode("cms-browser/assetsTreeLoader/cluster.config")) {
                    node.getNode("cms-browser/assetsTreeLoader/cluster.config").getProperty("wicket.model").remove();
                }

                Node browser = node.getNode("cms-browser");

                if(browser.hasNode("browserPerspective")) {
                    Node bp = browser.getNode("browserPerspective");
                    bp.getProperty("browser.viewers").remove();
                    bp.getProperty("editor.id").remove();
                    bp.getProperty("extension.list").remove();
                    bp.getProperty("model.document").remove();
                    bp.getProperty("model.folder").remove();
                    bp.getProperty("model.folder.root").remove();
                }

                // TODO: convert sections to navigation sections?
                if(browser.hasNode("browserPlugin")) {
                    browser.getNode("browserPlugin").remove();
                }

                if(browser.hasNode("documentsTreeLoader")) {
                    Node docTreeLoaderConfig = browser.getNode("documentsTreeLoader/cluster.config");
                    docTreeLoaderConfig.setProperty("wicket.model", "model.browse.collection");
                    docTreeLoaderConfig.getProperty("register.context.menu").remove();
                    docTreeLoaderConfig.setProperty("workflow.enabled", true);
                }

                if(browser.hasNode("configurationTreeLoader")) {
                    Node confTreeLoaderConfig = browser.getNode("configurationTreeLoader/cluster.config");
                    confTreeLoaderConfig.getProperty("wicket.model").remove();
                    confTreeLoaderConfig.getProperty("register.context.menu").remove();
                    confTreeLoaderConfig.setProperty("workflow.enabled", true);
                }

                if(browser.hasNode("imagesTreeLoader")) {
                    Node imagesTreeLoaderConfig = browser.getNode("imagesTreeLoader/cluster.config");
                    imagesTreeLoaderConfig.getProperty("wicket.model").remove();
                    imagesTreeLoaderConfig.getProperty("register.context.menu").remove();
                    imagesTreeLoaderConfig.setProperty("workflow.enabled", true);
                }

                if(browser.hasNode("assetsTreeLoader")) {
                    Node assetsTreeLoaderConfig = browser.getNode("assetsTreeLoader/cluster.config");
                    assetsTreeLoaderConfig.getProperty("register.context.menu").remove();
                    assetsTreeLoaderConfig.setProperty("workflow.enabled", true);
                }

                if(browser.hasNode("navigator")) {
                    Node nav = browser.getNode("navigator");
                    nav.getProperty("extension.browser").remove();
                    nav.getProperty("wicket.extensions").remove();
                    nav.setProperty("browser.id", "service.browse");
                    nav.setProperty("browser.viewers", "cms-folder-views");
                    nav.setProperty("model.default.path", "/content/documents");
                    nav.setProperty("model.document", "model.browse.document");
                    nav.setProperty("model.folder", "model.browse.folder");
                    nav.setProperty("search.viewers", "cms-search-views");
                    nav.setProperty("section.configuration", "service.browse.tree.configuration");
                    nav.setProperty("section.content", "service.browse.tree.content");
                    nav.setProperty("section.files", "service.browse.tree.files");
                    nav.setProperty("section.images", "service.browse.tree.images");
                    nav.setProperty("sections", new String[] { "section.content", "section.images", "section.files", "section.configuration" });
                    nav.setProperty("wicket.variant", "yui");
                }

                if(node.hasNode("cms-dashboard/dashboardLayout/yui.config")) {
                    Node dashLayout = node.getNode("cms-dashboard/dashboardLayout/yui.config");
                    dashLayout.setProperty("units", new String[] {"top", "left", "center", "right"});
                    dashLayout.setProperty("top", "id=top,height=23px");
                }

                if(node.hasNode("cms-editor")) {
                    node.getNode("cms-editor").remove();
                }

                if(node.hasNode("cms-preview")) {
                    node.getNode("cms-preview").remove();
                }

                if(node.hasNode("cms-pickers")) {
                    node.getNode("cms-pickers").remove();
                }

                // TODO: upgrade iso replace
                if(node.hasNode("cms-tree-views")) {
                    node.getNode("cms-tree-views").remove();
                }

                if(node.hasNode("cms-folder-views")) {
                    Node hidePubWf = node.getNode("cms-folder-views/hipposysedit:namespacefolder/root/filters").addNode("hideHippostdpubwfNamespace", "frontend:pluginconfig");
                    hidePubWf.setProperty("display", false);
                    hidePubWf.setProperty("path", "/hippo:namespaces/hippostdpubwf");
//                hidePubWf.getParent().orderBefore("hideHippostdpubwfNamespace", "hideHipposyseditNamespace");
                }

                for (NodeIterator cleanupEls = node.getNode("cms-services/htmlCleanerService/cleaner.config/hippohtmlcleaner:cleanup").getNodes("hippohtmlcleaner:cleanupElement"); cleanupEls.hasNext();) {
                    Node element = cleanupEls.nextNode();
                    if (element.hasProperty("hippohtmlcleaner:name") && "img".equals(element.getProperty("hippohtmlcleaner:name").getString())) {
                        Value[] attribs = element.getProperty("hippohtmlcleaner:attributes").getValues();
                        String[] newValues = new String[attribs.length + 1];
                        int i = 0;
                        for (Value value : attribs) {
                            newValues[ i++ ] = value.getString();
                        }
                        newValues[attribs.length] = "facetselect";
                        element.setProperty("hippohtmlcleaner:attributes", newValues);
                    }
                }

                node.getNode("cms-static/ajaxIndicator").remove();
                node.getNode("cms-static/pageLayoutBehavior").remove();
                node.getNode("cms-static/searchPlugin").remove();
                node.getNode("cms-static/yuiWebappBehavior").remove();

                Node surfAndEdit = node.getNode("cms-static").addNode("controllerPlugin", "frontend:plugin");
                surfAndEdit.setProperty("editor.id", "service.edit");
                surfAndEdit.setProperty("browser.id", "service.browse");
                surfAndEdit.setProperty("plugin.class", "org.hippoecm.frontend.plugins.cms.root.ControllerPlugin");

                Node root = node.getNode("cms-static/root");
                root.getProperty("extension.search").remove();
                root.setProperty("wicket.behavior", new String[] { "service.behavior.stylesheets" });
                root.setProperty("wicket.extensions", new String[] { "extension.header", "extension.center" });
                Node layout = root.addNode("yui.config", "frontend:pluginconfig");
                layout.setProperty("body.gutter", "0px 0px 0px 0px");
                layout.setProperty("header.gutter", "0px 0px 0px 0px");
                layout.setProperty("header.height", "50");
                layout.setProperty("root.id", "doc3");

                HippoSession session = (HippoSession) node.getSession();
                Node folderView = node.getNode("cms-folder-views/hippostd:folder");
                folderView.getNode("root/yui.config").remove();
                session.copy(folderView, node.getPath() + "/cms-folder-views/hippo:facetselect");

                for (NodeIterator fvi = node.getNode("cms-folder-views").getNodes(); fvi.hasNext();) {
                    Node fv= fvi.nextNode();
                    String name = fv.getName();
                    if (name.startsWith("hipposysedit:")) {
                        context.setName(fv, "hipposysedit_1_1:" + name.substring(name.indexOf(':') + 1));
                    }
                    if (fv.hasNode("root/filters")) {
                        Node filters = fv.getNode("root/filters");
                        Node ht = filters.addNode("hideTranslation", "frontend:pluginconfig");
                        ht.setProperty("display", false);
                        ht.setProperty("child", "hippo:translation");
                    }
                }
            }
        });

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostd:publishable") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                if(node.isNodeType("mix:versionable") && !node.isNodeType("hippostdpubwf_1_0:document")) {
                    node.addMixin("hippostdpubwf_1_0:document");
                    node.setProperty("hippostdpubwf_1_0:createdBy", "");
                    node.setProperty("hippostdpubwf_1_0:creationDate", calendar);
                    node.setProperty("hippostdpubwf_1_0:lastModifiedBy", "");
                    node.setProperty("hippostdpubwf_1_0:lastModificationDate", calendar);
                    if ("published".equals(node.getProperty("hippostd:state").getString())) {
                        node.setProperty("hippostdpubwf_1_0:publicationDate", calendar);
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.getNode("hippo").remove();
                node.getNode("hippostd").remove();
                node.getNode("hippogallery").remove();
                node.getNode("hipposysedit").remove();
                node.getProperty("system/Reference/hipposysedit_1_1:nodetype/hipposysedit_1_1:nodetype/hipposysedit_1_1:type").remove();
            }
        });
    }
}
