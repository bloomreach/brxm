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
package org.hippoecm.repository.upgrade;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.ext.UpdaterItemVisitor.PathVisitor;
import org.hippoecm.repository.ext.UpdaterItemVisitor.QueryVisitor;
import org.hippoecm.repository.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader13a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(Upgrader13a.class);

    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v13a");
        context.registerStartTag("v12a");
        context.registerStartTag("v12b");
        context.registerStartTag("v12c");
        context.registerEndTag("v13a");

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
                    for (NodeIterator iter = node.getNode("hippo:log").getNodes(); iter.hasNext();) {
                        iter.nextNode().remove();
                    }
                    context.setName(node.getNode("hippo:log"), "hippo:log");
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hipposysedit:field") {
            @Override
            public void leaving(final Node field, int level) throws RepositoryException {
                if (field.hasProperty("hipposysedit:name")) {
                    Property nameProperty = field.getProperty("hipposysedit:name");
                    if (field.getName().equals("hipposysedit:field")) {
                        context.setName(field, nameProperty.getString());
                    }
                    nameProperty.remove();
                }
            }
        });

        final Set<String> comparablePlugins = new TreeSet<String>();
        comparablePlugins.add("org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin");
        comparablePlugins.add("org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin");
        comparablePlugins.add("org.hippoecm.frontend.editor.plugins.resource.ImageDisplayPlugin");
        comparablePlugins.add("org.hippoecm.frontend.editor.plugins.ValueTemplatePlugin");
        comparablePlugins.add("org.hippoecm.frontend.editor.plugins.TextTemplatePlugin");
        comparablePlugins.add("org.hippoecm.frontend.plugins.xinha.XinhaNodePlugin");
        comparablePlugins.add("org.hippoecm.frontend.plugins.xinha.XinhaPlugin");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("editor:editable") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                if (node.hasNode("editor:templates")) {
                    Value compareToValue = node.getSession().getValueFactory().createValue("model.compareTo");
                    NodeIterator templates = node.getNode("editor:templates").getNodes();
                    while (templates.hasNext()) {
                        Node template = templates.nextNode();
                        if (!template.isNodeType("frontend:plugincluster")) {
                            continue;
                        }
                        // expect at the least a wicket.model reference
                        if (!template.hasProperty("frontend:references")) {
                            continue;
                        }

                        boolean handle = false;
                        NodeIterator plugins = template.getNodes();
                        while (plugins.hasNext()) {
                            Node plugin = plugins.nextNode();
                            if (!plugin.isNodeType("frontend:plugin")) {
                                continue;
                            }
                            if (plugin.hasProperty("plugin.class")) {
                                String clazz = plugin.getProperty("plugin.class").getString();
                                if (comparablePlugins.contains(clazz)) {
                                    handle = true;
                                    break;
                                }
                            }
                        }
                        if (handle) {
                            Value[] references = template.getProperty("frontend:references").getValues();
                            Value[] newRefs = new Value[references.length + 1];
                            System.arraycopy(references, 0, newRefs, 0, references.length);
                            newRefs[references.length] = compareToValue;
                            template.setProperty("frontend:references", newRefs);

                            plugins = template.getNodes();
                            while (plugins.hasNext()) {
                                Node plugin = plugins.nextNode();
                                if (!plugin.isNodeType("frontend:plugin")) {
                                    continue;
                                }
                                if (plugin.hasProperty("plugin.class")) {
                                    String clazz = plugin.getProperty("plugin.class").getString();
                                    if (comparablePlugins.contains(clazz)) {
                                        plugin.setProperty("model.compareTo", "${model.compareTo}");
                                        if (!plugin.hasProperty("mode")) {
                                            plugin.setProperty("mode", "${mode}");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        //Picker Update
        context.registerVisitor(new PathVisitor("/hippo:namespaces/hippo") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("mirror")) {
                    Node mirror = node.getNode("mirror");
                    if (mirror.isNodeType("editor:editable")) {
                        for (NodeIterator templates = mirror.getNode("editor:templates").getNodes(); templates.hasNext();) {
                            Node template = templates.nextNode();
                            if (template.isNodeType("frontend:plugincluster")) {
                                Node mtp = null;
                                for (NodeIterator plugins = template.getNodes(); plugins.hasNext();) {
                                    Node plugin = plugins.nextNode();
                                    if (!plugin.isNodeType("frontend:plugin")) {
                                        continue;
                                    }
                                    if (plugin.hasProperty("plugin.class")
                                            && "org.hippoecm.frontend.editor.plugins.linkpicker.MirrorTemplatePlugin"
                                                    .equals(plugin.getProperty("plugin.class").getString())) {
                                        mtp = plugin;
                                        break;
                                    }
                                }
                                if (mtp != null) {
                                    mtp.setProperty("last.visited.enabled", "${last.visited.enabled}");
                                    mtp.setProperty("last.visited.key", "${last.visited.key}");
                                    mtp.setProperty("last.visited.nodetypes", "${last.visited.nodetypes}");
                                    mtp.setProperty("base.uuid", "${base.uuid}");
                                    mtp.setProperty("cluster.name", "${cluster.name}");
                                    template.setProperty("cluster.name", "cms-pickers/documents");
                                    Set<String> properties = new TreeSet<String>();
                                    for (Value val : template.getProperty("frontend:properties").getValues()) {
                                        properties.add(val.getString());
                                    }
                                    properties.add("cluster.name");
                                    properties.add("base.uuid");
                                    properties.add("last.visited.enabled");
                                    properties.add("last.visited.key");
                                    properties.add("last.visited.nodetypes");
                                    template.setProperty("frontend:properties", properties.toArray(new String[properties.size()]));
                                }
                            }
                        }
                    }
                }
            }
        });
        
        context.registerVisitor(new PathVisitor("/hippo:namespaces/hipposysedit") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }
        });
        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:initialize/templateeditor-hipposysedit") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.QueryVisitor(
                "//element(*,frontend:pluginconfig)[@encoding.node]", Query.XPATH) {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log
                        .error("encoding.node property on "
                                + node.getPath()
                                + " no longer supported, please set /hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs/@encoding.node property instead");
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.QueryVisitor(
                "//element(*,frontend:pluginconfig)[@encoding.display]", Query.XPATH) {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log
                        .error("encoding.display property on "
                                + node.getPath()
                                + " no longer supported, please set /hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs/@encoding.display property instead");
            }
        });

        //Picker Update
        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log.info("Removing the cms-pickers node from " + node.getPath());
                node.getNode("cms-pickers").remove();
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("cms-pickers")) {
                    log.info("Removing the cms-pickers node from " + node.getPath());
                    node.getNode("cms-pickers").remove();
                }
            }
        });

        for (String path : new String[] { "/hippo:configuration/hippo:frontend/login",
                "/hippo:configuration/hippo:frontend/cms/cms-headshortcuts",
                "/hippo:configuration/hippo:frontend/cms/cms-reports",
                "/hippo:configuration/hippo:frontend/cms/cms-browser/browserLayout",
                "/hippo:configuration/hippo:frontend/cms/cms-browser/editorTabUnit",
                "/hippo:configuration/hippo:frontend/cms/cms-browser/editorTabTopUnit",
                "/hippo:configuration/hippo:frontend/cms/cms-browser/tabbedEditorLayout",
                "/hippo:configuration/hippo:domains/frontendconfig/frontent-plugins/exclude-management" }) {
            context.registerVisitor(new PathVisitor(path) {
                @Override
                protected void leaving(Node node, int level) throws RepositoryException {
                    node.remove();
                }
            });
        }
        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (String element : new String[] { "cms-login", "cms-reports" }) {
                    if (node.hasNode(element)) {
                        node.getNode(element).remove();
                    }
                }
            }
        });

        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-services") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node settings = node.addNode("settingsService", "frontend:plugin");
                settings.setProperty("plugin.class", "org.hippoecm.frontend.service.settings.SettingsStorePlugin");
                Node codecs = settings.addNode("codecs", "frontend:pluginconfig");
                codecs.setProperty("encoding.display", "org.hippoecm.repository.api.StringCodecFactory$IdentEncoding");
                codecs.setProperty("encoding.node", "org.hippoecm.repository.api.StringCodecFactory$UriEncoding");

                if (node.hasNode("htmlCleanerService")) {
                    Node cleanup = node.getNode("htmlCleanerService/cleaner.config/hippohtmlcleaner_2_1:cleanup");
                    for (NodeIterator elements = cleanup.getNodes("hippohtmlcleaner_2_1:cleanupElement"); elements
                            .hasNext();) {
                        Node element = elements.nextNode();
                        if ("img".equals(element.getProperty("hippohtmlcleaner_2_1:name").getString())
                                && element.hasProperty("hippohtmlcleaner_2_1:attributes")) {
                            Property attrs = element.getProperty("hippohtmlcleaner_2_1:attributes");
                            Value[] values = attrs.getValues();
                            if (values.length > 0) {
                                int idx = -1;
                                for (int i = 0; i < values.length; i++) {
                                    if ("facetselect".equals(values[i].getString())) {
                                        idx = i;
                                        break;
                                    }
                                }
                                if (idx >= 0) {
                                    Value[] newValues = new Value[values.length - 1];
                                    System.arraycopy(values, 0, newValues, 0, idx);
                                    System.arraycopy(values, idx + 1, newValues, idx, values.length - idx - 1);
                                    attrs.setValue(newValues);
                                }
                            }
                        }
                    }

                    //HREPTWO-3952 : HtmlCleaner is lenient by default
                    if (node.hasNode("htmlCleanerService/cleaner.config")) {
                        Node conf = node.getNode("htmlCleanerService/cleaner.config");
                        if (!conf.hasProperty("lenient")) {
                            conf.setProperty("lenient", true);
                        }
                    }
                }
            }
        });

        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-static") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.getNode("headerPlugin").remove();
                node.getNode("footerPlugin").remove();
                node.getNode("adminLayout").remove();
                node.getNode("browseShortcutsListPlugin").remove();
                node.getNode("browseShortcutsPlugin").remove();
                node.getNode("centerPlugin").remove();
                node.getNode("browserSpecificStylesheetsBehavior").remove();
                {
                    Node adminPerspective = node.getNode("adminPerspective");
                    adminPerspective.getProperty("wicket.behavior").remove();
                    Node layout = adminPerspective.addNode("layout.wireframe", "frontend:pluginconfig");
                    layout
                            .setProperty("center",
                                    "id=hippo-controlpanel-perspective-center,body=hippo-controlpanel-perspective-center-body,scroll=true");
                    layout.setProperty("linked.with.parent", true);
                    layout.setProperty("root.id", "hippo-controlpanel-perspective-wrapper");
                    layout
                            .setProperty("top",
                                    "id=hippo-controlpanel-perspective-top,body=hippo-controlpanel-perspective-top-body,height=35");
                }
                node.getNode("logoutPlugin").remove();
                {
                    Node root = node.getNode("root");
                    root.getProperty("extension.center").remove();
                    root.getProperty("extension.header").remove();
                    root.getProperty("wicket.behavior").remove();
                    root.getProperty("wicket.extensions").remove();
                    root.setProperty("tabs", "service.tab");
                    root.setProperty("browsers", new String[] { "browser.ie", "browser.ie7" });
                    Node ie = root.addNode("browser.ie", "frontend:pluginconfig");
                    ie.setProperty("stylesheets", new String[] { "skin/screen_ie.css" });
                    ie.setProperty("user.agent", "ie");
                    Node ie7 = root.addNode("browser.ie7", "frontend:pluginconfig");
                    ie7.setProperty("stylesheets", new String[] { "skin/screen_ie7.css" });
                    ie7.setProperty("user.agent", "ie");
                    ie7.setProperty("major.version", "7");
                    Node wf = root.addNode("layout.wireframe", "frontend:pluginconfig");
                    wf.setProperty("center", "id=tabbed-panel-layout-center,height=1000");
                    wf.setProperty("left", "id=tabbed-panel-layout-left,width=50");
                    wf.setProperty("linked.with.parent", true);
                    wf.setProperty("root.id", "tabbed-panel-layout");
                    root.getNode("yui.config").remove();
                }
                node.getNode("tabLayout").remove();
                node.getNode("tabTopUnit").remove();
                node.getNode("tabUnit").remove();
            }
        });

        //Documentlisting wide-view update

        context.registerVisitor(new PathVisitor(
                "/hippo:configuration/hippo:frontend/cms/cms-browser/browserPerspective") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasProperty("extension.editor")) {
                    Property prop = node.getProperty("extension.editor");
                    Value editor = prop.getValue();
                    prop.remove();
                    node.setProperty("center", editor);
                }
                if (node.hasProperty("extension.navigator")) {
                    Property prop = node.getProperty("extension.navigator");
                    Value navigator = prop.getValue();
                    prop.remove();
                    node.setProperty("left", navigator);
                }
                if (node.hasProperty("wicket.behavior")) {
                    node.getProperty("wicket.behavior").remove();
                }
                if (node.hasProperty("wicket.extensions")) {
                    node.getProperty("wicket.extensions").remove();
                }
                node.setProperty("top", "service.browse.tabscontainer");
                node.setProperty("model.document", "model.browse.document");

                Node layout = node.addNode("layout.wireframe", "frontend:pluginconfig");
                layout
                        .setProperty("center",
                                "id=browse-perspective-center,body=browse-perspective-center-body,min.width=400,scroll=false,gutter=0px 0px 0px 0px");
                layout
                        .setProperty(
                                "left",
                                "id=browse-perspective-left,body=browse-perspective-left-body,scroll=false,width=400,gutter=0px 0px 0px 0px,expand.collapse.enabled=true");
                layout.setProperty("linked.with.parent", true);
                layout.setProperty("root.id", "browse-perspective-wrapper");
                layout.setProperty("top", "id=browse-perspective-top,height=25");
                layout.setProperty("default.expanded.unit", "left");
            }
        });
        context.registerVisitor(new PathVisitor(
                "/hippo:configuration/hippo:frontend/cms/cms-browser/editorManagerPlugin") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node compare = node.addNode("cluster.compare.options", "frontend:pluginconfig");
                compare.setProperty("browser.id", "service.browse");
                compare.setProperty("editor.id", "service.edit");
                compare.setProperty("wicket.behavior", "service.behavior.editor.tabs.center");
                compare.setProperty("wicket.id", "service.editor.tab");
                if (node.hasProperty("cluster.edit.name")) {
                    node.getProperty("cluster.edit.name").remove();
                }
                if (node.hasProperty("cluster.preview.name")) {
                    node.getProperty("cluster.preview.name").remove();
                }
            }
        });
        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-browser/navigatorLayout") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("yui.config")) {
                    Node config = node.getNode("yui.config");
                    config.setProperty("left",
                            "id=navigator-left,body=navigator-left-body,width=200,zindex=2,min.width=100,resize=true");
                    config.setProperty("center",
                            "id=navigator-center,body=navigator-center-body,width=250,min.width=100");
                    config.getProperty("top").remove();
                    config.getProperty("units").remove();
                }
            }
        });
        context
                .registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-browser/tabbedEditorTabs") {
                    @Override
                    protected void leaving(Node node, int level) throws RepositoryException {
                        node.getProperty("tabbedpanel.behavior").remove();
                        node.getProperty("wicket.behavior").remove();
                        node.setProperty("tabs.container.id", "service.browse.tabscontainer");
                    }
                });
        context
                .registerVisitor(new PathVisitor(
                        "/hippo:configuration/hippo:frontend/cms/cms-dashboard/dashboardLayout") {
                    @Override
                    protected void leaving(Node node, int level) throws RepositoryException {
                        if (node.hasNode("yui.config")) {
                            node.getProperty("yui.config/center").setValue(
                                    "id=center,scroll=true,width=33%,body=center-body,gutter=0px 1px 1px 0px");
                            node.getProperty("yui.config/right").setValue(
                                    "id=right,width=33%,body=right-body,scroll=true,gutter=0px 1px 1px 0px");
                            node.getProperty("yui.config/left").setValue(
                                    "id=left,width=33%,body=left-body,gutter=0px 1px 1px 1px");
                            node.getProperty("yui.config/top").setValue("id=top,height=24");
                            node.getProperty("yui.config/units").remove();
                        }
                    }
                });
        for (String perspective : new String[] { "/hippo:configuration/hippo:frontend/cms/cms-editor/editPerspective",
                "/hippo:configuration/hippo:frontend/cms/cms-preview/previewPerspective" }) {
            context.registerVisitor(new PathVisitor(perspective) {
                @Override
                protected void leaving(Node node, int level) throws RepositoryException {
                    Node layout = node.getNode("yui.config");
                    context.setName(layout, "layout.wireframe");
                    layout.getProperty("units").remove();
                }
            });
        }
        for (String workflowPlugin : new String[] {
                "/hippo:configuration/hippo:frontend/cms/cms-editor/workflowPlugin",
                "/hippo:configuration/hippo:frontend/cms/cms-preview/workflowPlugin", }) {
            context.registerVisitor(new PathVisitor(workflowPlugin) {
                @Override
                protected void leaving(Node node, int level) throws RepositoryException {
                    if (node.hasProperty("model.id")) {
                        node.getProperty("model.id").remove();
                    }
                }
            });
        }

        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-folder-views") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("hippostd:directory/root")) {
                    Node root = node.getNode("hippostd:directory/root");
                    root.setProperty("expand.collapse.supported", true);
                }
                if (node.hasNode("hippostd:folder/root")) {
                    Node root = node.getNode("hippostd:folder/root");
                    root.setProperty("expand.collapse.supported", true);
                }
                if (node.hasNode("hipposysedit:namespace")) {
                    context.setName(node.getNode("hipposysedit:namespace"), "hipposysedit_1_2:namespace");
                }
                if (node.hasNode("hipposysedit:namespacefolder")) {
                    context.setName(node.getNode("hipposysedit:namespacefolder"), "hipposysedit_1_2:namespacefolder");
                }
            }
        });

        context.registerVisitor(new QueryVisitor("//element(*,frontend:plugin)[@plugin.class='org.hippoecm.frontend.plugins.reviewedactions.DefaultWorkflowPlugin']", "xpath") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                node.setProperty("browser.id", "${browser.id}");
            }
        });
        
        context.registerVisitor(new PathVisitor(
                "/hippo:configuration/hippo:domains/defaultread/hippo-facetsubsearch/type-hippo-facetsubsearch") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasProperty("hipposys:value")
                        && node.getProperty("hipposys:value").getString().equals("hippo:facetsubsearch")) {
                    node.setProperty("hipposys:value", "hipposys:facetsubsearch");
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposysedit", getClass()
                .getClassLoader().getResourceAsStream("hipposysedit.cnd")));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippohtmlcleaner", getClass()
                .getClassLoader().getResourceAsStream("hippohtmlcleaner.cnd")));

        try {
            Workspace workspace = context.getWorkspace();
            for (String subtypedNamespace : subTypedNamespaces(workspace)) {
                String oldUri = workspace.getNamespaceRegistry().getURI(subtypedNamespace);
                String newUri = VersionNumber.versionFromURI(oldUri).next().versionToURI(oldUri);
                log.info("Derived namespace " + subtypedNamespace + " " + oldUri + " -> " + newUri);
                workspace.getNamespaceRegistry().registerNamespace(subtypedNamespace, newUri);
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, subtypedNamespace, "-", null));
            }
        } catch (NamespaceException ex) {
            log.error("failure in conversion script", ex);
        } catch (RepositoryException ex) {
            log.error("failure in conversion script", ex);
        }

        //HREPTWO-4159 - implement AjaxUpload; new Flash upload can be restricted to only allow files with certain extensions           
        context.registerVisitor(new PathVisitor(
                "/hippo:configuration/hippo:workflows/threepane/image-gallery/frontend:renderer") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (!node.hasProperty("file.extensions")) {
                    node.setProperty("file.extensions", new String[] { "*.jpg", "*.jpeg", "*.gif", "*.png" });
                }
            }
        });
    }

    private Collection<String> subTypedNamespaces(Workspace workspace) throws RepositoryException {
        Set<String> knownNamespaces = new HashSet<String>();
        LinkedList<String> subtypedNamespaces = new LinkedList<String>();
        Set<String> skippedNamespaces = new HashSet<String>();
        knownNamespaces.add("hipposysedit");
        knownNamespaces.add("hippohtmlcleaner");
        skippedNamespaces.addAll(knownNamespaces);
        NodeTypeManager ntMgr = workspace.getNodeTypeManager();
        boolean rerun;
        do {
            rerun = false;
            for (NodeTypeIterator ntiter = ntMgr.getAllNodeTypes(); ntiter.hasNext();) {
                NodeType nt = ntiter.nextNodeType();
                String ntName = nt.getName();
                if (ntName.contains(":")) {
                    String ntNamespace = ntName.substring(0, ntName.indexOf(":"));
                    // if the namespace (x) is not known, but one of the supertypes of the type is in a
                    // known namespace (y) then add the namespace (x) to the list of known namespaces and restart.
                    if (!knownNamespaces.contains(ntNamespace)) {
                        Set<NodeType> dependencies = new HashSet<NodeType>();
                        for (NodeType superType : nt.getSupertypes())
                            dependencies.add(superType);
                        for (NodeDefinition childDef : nt.getDeclaredChildNodeDefinitions()) {
                            if (childDef.getDefaultPrimaryType() != null)
                                dependencies.add(childDef.getDefaultPrimaryType());
                            for (NodeType childNodeType : childDef.getRequiredPrimaryTypes())
                                dependencies.add(childNodeType);
                        }
                        for (NodeType superType : dependencies) {
                            String superName = superType.getName();
                            if (superName.contains(":")) {
                                String superNamespace = superName.substring(0, superName.indexOf(":"));
                                if (knownNamespaces.contains(superNamespace)) {
                                    knownNamespaces.add(ntNamespace);
                                    if (!skippedNamespaces.contains(ntNamespace)) {
                                        subtypedNamespaces.addFirst(ntNamespace);
                                    }
                                    rerun = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (rerun)
                    break;
            }
        } while (rerun);
        return subtypedNamespaces;
    }
}
