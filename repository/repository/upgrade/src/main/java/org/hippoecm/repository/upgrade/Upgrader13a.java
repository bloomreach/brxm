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
package org.hippoecm.repository.upgrade;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.jcr.NamespaceException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterItemVisitor.PathVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import java.io.InputStreamReader;

public class Upgrader13a implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(Upgrader13a.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v13a");
        context.registerStartTag("v12a");
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
                    for(NodeIterator iter=node.getNode("hippo:log").getNodes(); iter.hasNext(); ) {
                        iter.nextNode().remove();
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hipposysedit:nodetype") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                context.setName(node, "hipposysedit:nodetype");
                //context.setPrimaryNodeType(node, "hipposysedit:nodetype");
                for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                    Node field = iter.nextNode();
                    context.setPrimaryNodeType(field, "hipposysedit_1_2:field");
                    if(field.hasProperty("hipposysedit:name")) {
                        Property nameProperty = field.getProperty("hipposysedit:name");
                        if(field.getName().equals("hipposysedit:field")) {
                            context.setName(field, nameProperty.getString());
                        }
                        nameProperty.remove();
                    }
                }
            }
        });

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
                                if ("org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin".equals(clazz)
                                        || "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin"
                                                .equals(clazz)) {
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
                                    if ("org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin".equals(clazz)
                                            || "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin"
                                                    .equals(clazz)) {
                                        plugin.setProperty("model.compareTo", "${model.compareTo}");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.QueryVisitor("//element(*,frontend:pluginconfig)[@encoding.node]", Query.XPATH) {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log.error("encoding.node property on "+node.getPath()+" no longer supported, please set /hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs/@encoding.node property instead");
            } 
        });

        context.registerVisitor(new UpdaterItemVisitor.QueryVisitor("//element(*,frontend:pluginconfig)[@encoding.display]", Query.XPATH) {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                log.error("encoding.dos[;ay property on "+node.getPath()+" no longer supported, please set /hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs/@encoding.display property instead");
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

        //Documentlisting wide-view update
        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-browser") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if(node.hasNode("browserPerspective/layout.wireframe")) {
                    Node layout = node.getNode("browserPerspective/layout.wireframe");
                    layout.setProperty("left", "id=browse-perspective-left,body=browse-perspective-left-body,scroll=false,width=400,gutter=0px 0px 0px 0px,expand.collapse.enabled=true");
                    layout.setProperty("center", "id=browse-perspective-center,body=browse-perspective-center-body,min.width=400,scroll=false,gutter=0px 0px 0px 0px");
                }
                if(node.hasNode("navigatorLayout/yui.config")) {
                    Node config = node.getNode("navigatorLayout/yui.config");
                    config.setProperty("left", "id=navigator-left,body=navigator-left-body,width=200,zindex=2,min.width=100,resize=true");
                    config.setProperty("center", "id=navigator-center,body=navigator-center-body,width=250,min.width=100");
                }
            }
        });

        context.registerVisitor(new PathVisitor("/hippo:configuration/hippo:frontend/cms/cms-folder-views") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if(node.hasNode("hippostd:directory/root")) {
                    Node root = node.getNode("hippostd:directory/root");
                    root.setProperty("expand.collapse.supported", true);
                }
                if (node.hasNode("hippostd:folder/root")) {
                    Node root = node.getNode("hippostd:folder/root");
                    root.setProperty("expand.collapse.supported", true);
                }
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippo", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippo.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposys", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposys.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposysedit", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposysedit.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippofacnav", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippofacnav.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostd", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippostd.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostdpubwf", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippostdpubwf.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hipposched", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hipposched.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippoldap", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippoldap.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippogallery", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippogallery.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "frontend", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("frontend.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "editor", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("editor.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippohtmlcleaner", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("hippohtmlcleaner.cnd"))));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "reporting", "-",
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream("reporting.cnd"))));

        try {
            Workspace workspace = context.getWorkspace();
            for(String subtypedNamespace : subTypedNamespaces(workspace)) {
                String oldUri = workspace.getNamespaceRegistry().getURI(subtypedNamespace);
                String newUri = VersionNumber.versionFromURI(oldUri).next().versionToURI(oldUri);
                log.info("Derived namespace " + subtypedNamespace + " " + oldUri + " -> " + newUri);
                workspace.getNamespaceRegistry().registerNamespace(subtypedNamespace, newUri);
                if(subtypedNamespace.equals("defaultcontent"))
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, subtypedNamespace, "-", null));
            }
        } catch (NamespaceException ex) {
            log.error("failure in conversion script", ex);
        } catch (RepositoryException ex) {
            log.error("failure in conversion script", ex);
        }
    }


    private Collection<String> subTypedNamespaces(Workspace workspace) throws RepositoryException {
        Set<String> knownNamespaces = new HashSet<String>();
        LinkedList<String> subtypedNamespaces = new LinkedList<String>();
        Set<String> skippedNamespaces = new HashSet<String>();
        knownNamespaces.add("hippo");
        skippedNamespaces.add("hippo");
        skippedNamespaces.add("hipposys");
        skippedNamespaces.add("hipposysedit");
        skippedNamespaces.add("hippofacnav");
        skippedNamespaces.add("hippostd");
        skippedNamespaces.add("hippostdpubwf");
        skippedNamespaces.add("hipposched");
        skippedNamespaces.add("hippoldap1");
        skippedNamespaces.add("hippogallery");
        skippedNamespaces.add("frontend");
        skippedNamespaces.add("editor");
        skippedNamespaces.add("hippohtmlcleaner");
        skippedNamespaces.add("reporting");
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
                            if(childDef.getDefaultPrimaryType() != null)
                                dependencies.add(childDef.getDefaultPrimaryType());
                            for(NodeType childNodeType : childDef.getRequiredPrimaryTypes())
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
