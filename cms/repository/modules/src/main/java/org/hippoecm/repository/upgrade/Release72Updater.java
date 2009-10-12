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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.util.TraversingItemVisitor;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.hippoecm.repository.util.VersionNumber;

public class Release72Updater implements UpdaterModule {
    private static final String[][] rules = {
        {"type", "hippo:remodel", "hipposysedit_1_0:remodel"},
        {"field", "hippo:uri", "hipposysedit_1_0:uri"},
        {"type", "hippo:field", "hipposysedit_1_0:field"},
        {"field", "hippo:name", "hipposysedit_1_0:name"},
        {"field", "hippo:path", "hipposysedit_1_0:path"},
        {"field", "hippo:type", "hipposysedit_1_0:type"},
        {"field", "hippo:multiple", "hipposysedit_1_0:multiple"},
        {"field", "hippo:mandatory", "hipposysedit_1_0:mandatory"},
        {"field", "hippo:ordered", "hipposysedit_1_0:ordered"},
        {"field", "hippo:primary", "hipposysedit_1_0:primary"},
        {"type", "hippo:nodetype", "hipposysedit_1_0:nodetype"},
        {"field", "hippo:type", "hipposysedit_1_0:type "},
        {"field", "hippo:supertype", "hipposysedit_1_0:supertype"},
        {"field", "hippo:node", "hipposysedit_1_0:node"},
        {"field", "hippo:mixin", "hipposysedit_1_0:mixin"},
        {"child", "hippo:field", "hipposysedit_1_0:field"},
        // {"type", "hippo:templatetype", "hipposysedit_1_0:templatetype"},
        {"type", "hippo:initializeitem", "hippo_2_0:initializeitem"},
        {"type", "hippo:softdocument", "hipposys_1_0:softdocument"},
        {"field", "hippo:uuid", "hipposys_1_0:uuid"},
        {"type", "hippo:request", "hipposys_1_0:request"},
        {"type", "hippo:implementation", "hipposys_1_0:implementation"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:serialver", "hipposys_1_0:serialver"},
        {"type", "hippo:type", "hipposys_1_0:type"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"type", "hippo:types", "hipposys_1_0:types"},
        {"type", "hippo:workflow", "hipposys_1_0:workflow"},
        {"field", "hippo:workflow", "hipposys_1_0:classname"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"child", "hippo:config", "hipposys_1_0:config"},
        {"type", "hippo:workflowcategory", "hipposys_1_0:workflowcategory"},
        {"type", "hippo:workflowfolder", "hipposys_1_0:workflowfolder"},
        {"type", "hippo:ocmquery", "hipposys_1_0:ocmquery"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"type", "hippo:ocmqueryfolder", "hipposys_1_0:ocmqueryfolder"},
        {"type", "hippo:queryfolder", "hipposys_1_0:queryfolder"},
        {"type", "hippo:basequeryfolder", "hipposys_1_0:basequeryfolder"},
        {"type", "hippo:propertyreference", "hipposys_1_0:propertyreference"},
        {"type", "hippo:relativepropertyreference", "hipposys_1_0:relativepropertyreference"},
        {"field", "hippo:relPath", "hipposys_1_0:relPath"},
        {"type", "hippo:resolvepropertyreference", "hipposys_1_0:resolvepropertyreference"},
        {"field", "hippo:relPath", "hipposys_1_0:relPath"},
        {"type", "hippo:builtinpropertyreference", "hipposys_1_0:builtinpropertyreference"},
        {"field", "hippo:method", "hipposys_1_0:method"},
        {"type", "hippo:propertyreferences", "hipposys_1_0:propertyreferences"},
        {"type", "hippo:deriveddefinition", "hipposys_1_0:deriveddefinition"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:serialver", "hipposys_1_0:serialver"},
        {"child", "hippo:accessed", "hipposys_1_0:accessed"},
        {"child", "hippo:derived", "hipposys_1_0:derived"},
        {"type", "hippo:derivativesfolder", "hipposys_1_0:derivativesfolder"},
        {"type", "hippo:initializefolder", "hippo_2_0:initializefolder"},
        {"type", "hippo:temporaryfolder", "hipposys_1_0:temporaryfolder"},
        {"type", "hippo:applicationfolder", "hipposys_1_0:applicationfolder"},
        {"type", "hippo:configuration", "hipposys_1_0:configuration"},
        {"type", "hippo:accessmanager", "hipposys_1_0:accessmanager"},
        {"field", "hippo:permissioncachesize", "hipposys_1_0:permissioncachesize"},
        {"type", "hippo:user", "hipposys_1_0:user"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:active", "hipposys_1_0:active"},
        {"field", "hippo:password", "hipposys_1_0:password"},
        {"field", "hippo:passkey", "hipposys_1_0:passkey"},
        {"field", "hippo:lastlogin", "hipposys_1_0:lastlogin"},
        {"type", "hippo:externaluser", "hipposys_1_0:externaluser"},
        {"field", "hippo:lastsync", "hipposys_1_0:lastsync"},
        {"type", "hippo:group", "hipposys_1_0:group"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:members", "hipposys_1_0:members"},
        {"field", "hippo:groups", "hipposys_1_0:groups"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:externalgroup", "hipposys_1_0:externalgroup"},
        {"field", "hippo:syncdate", "hipposys_1_0:syncdate"},
        {"type", "hippo:role", "hipposys_1_0:role"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"field", "hippo:roles", "hipposys_1_0:roles"},
        {"field", "hippo:jcrread", "hipposys_1_0:jcrread"},
        {"field", "hippo:jcrwrite", "hipposys_1_0:jcrwrite"},
        {"field", "hippo:jcrremove", "hipposys_1_0:jcrremove"},
        {"type", "hippo:externalrole", "hipposys_1_0:externalrole"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"type", "hippo:authrole", "hipposys_1_0:authrole"},
        {"field", "hippo:users", "hipposys_1_0:users"},
        {"field", "hippo:groups", "hipposys_1_0:groups"},
        {"field", "hippo:role", "hipposys_1_0:role"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:facetrule", "hipposys_1_0:facetrule"},
        {"field", "hippo:facet", "hipposys_1_0:facet"},
        {"field", "hippo:value", "hipposys_1_0:value"},
        {"field", "hippo:type", "hipposys_1_0:type"},
        {"field", "hippo:equals", "hipposys_1_0:equals"},
        {"field", "hippo:filter", "hipposys_1_0:filter"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:domainrule", "hipposys_1_0:domainrule"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:domain", "hipposys_1_0:domain"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:userprovider", "hipposys_1_0:userprovider"},
        {"field", "hippo:dirlevels", "hipposys_1_0:dirlevels"},
        {"type", "hippo:groupprovider", "hipposys_1_0:groupprovider"},
        {"field", "hippo:dirlevels", "hipposys_1_0:dirlevels"},
        {"type", "hippo:roleprovider", "hipposys_1_0:roleprovider"},
        {"type", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:userprovider", "hipposys_1_0:userprovider"},
        {"child", "hippo:groupprovider", "hipposys_1_0:groupprovider"},
        {"child", "hippo:roleprovider", "hipposys_1_0:roleprovider"},
        {"type", "hippo:userfolder", "hipposys_1_0:userfolder"},
        {"type", "hippo:groupfolder", "hipposys_1_0:groupfolder"},
        {"type", "hippo:rolefolder", "hipposys_1_0:rolefolder"},
        {"type", "hippo:domainfolder", "hipposys_1_0:domainfolder"},
        {"type", "hippo:securityfolder", "hipposys_1_0:securityfolder"},
        {"field", "hippo:userspath", "hipposys_1_0:userspath"},
        {"field", "hippo:groupspath", "hipposys_1_0:groupspath"},
        {"field", "hippo:rolespath", "hipposys_1_0:rolespath"},
        {"field", "hippo:domainspath", "hipposys_1_0:domainspath"},
        {"child", "hippo:accessmanager", "hipposys_1_0:accessmanager"},
        {"type", "hippo:namespace", "hipposysedit_1_0:namespace"},
        {"type", "hippo:namespacefolder", "hipposysedit_1_0:namespacefolder"},
        {"type", "hippo:resource", "hippo_2_0:resource"},
        {"type", "hippo:query", "hippo_2_0:query"},
        {"type", "hippo:derived", "hippo_2_0:derived"},
        {"type", "hippo:document", "hippo_2_0:document"},
        {"type", "hippo:handle", "hippo_2_0:handle"},
        {"type", "hippo:hardhandle", "hippo_2_0:hardhandle"},
        {"type", "hippo:harddocument", "hippo_2_0:harddocument"},
        {"type", "hippo:facetresult", "hippo_2_0:facetresult"},
        {"type", "hippo:facetbasesearch", "hippo_2_0:facetbasesearch"},
        {"type", "hippo:facetsearch", "hippo_2_0:facetsearch"},
        {"type", "hippo:facetselect", "hippo_2_0:facetselect"},
        {"type", "hippo:mirror", "hippo_2_0:mirror"},
        {"type", "hippo:facetlink", null},
        //{"type","hippo:translation","hipponew:translation"},
        //{"type","hippo:translated","hipponew:translated"},

        {"type", "frontend:workflow", "frontend_2_0:workflow"},
        {"field", "hippo:workflow", "hipposys_1_0:classname"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"child", "hippo:config", "hipposys_1_0:config"},
        //{"type", "frontend:user", "frontend:user2"},
        //{"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        //{"field", "hippo:active", "hipposys_1_0:active"},
        //{"field", "hippo:password", "hipposys_1_0:password"},
        //{"field", "hippo:passkey", "hipposys_1_0:passkey"},
        //{"field", "hippo:lastlogin", "hipposys_1_0:lastlogin"},

        {"type", "hippo:implementation", "hipposys_1_0:implementation"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
    };

    public void register(final UpdaterContext context) {
        context.registerName("upgrade");
        context.registerStartTag("m13");
        context.registerEndTag("tag209");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:derived") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasProperty("hippo:related")) {
                    node.getProperty("hippo:related").remove();
                }
                if (node.hasProperty("hippo:compute")) {
                    node.getProperty("hippo:compute").remove();
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                /*
                 * The removal of the entire /hippo:log tree seems to be appropriate.  This is relatively volatile data as
                 * this is a sliding log file with the oldest entries being removed automatically.  Combine this with the
                 * fact that old entries might not contain the same information and the effort of converting data which is
                 * going to be removed quickly is unnecessary.
                 */
                node.getNode("hippo:log").remove();

                node.getNode("live").remove();
                node.getNode("preview").remove();
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:namespacefolder") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                context.setPrimaryNodeType(node, "hipposysedit_1_0:namespacefolder");
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.isNodeType("hippo:namespace")) {
                        context.setPrimaryNodeType(child, "hipposysedit_1_0:namespace");
                    }
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:templatetype") {

            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                // Should a new nodetype descriptor be created?
                boolean convert = false;
                String prefix = node.getParent().getName();
                String uri = null;
                String newUri = null;
                if (node.getDepth() > 0 && node.getParent().isNodeType("hipposysedit_1_0:namespace")
                        && !"system".equals(prefix)) {
                    uri = node.getSession().getNamespaceURI(prefix);
                    VersionNumber version = new VersionNumber(uri.substring(uri.lastIndexOf("/") + 1));
                    newUri = uri.substring(0, uri.lastIndexOf('/') + 1) + version.next().toString();
                    convert = true;
                    try {
                        node.getSession().getNamespacePrefix(newUri);
                    } catch (NamespaceException ex) {
                        convert = false;
                    }
                }

                context.setPrimaryNodeType(node, "hipposysedit_1_0:templatetype");
                Node child = node.getNode("hippo:nodetype");
                Node current = null;
                for (NodeIterator nodetypeVersionIter = child.getNodes(child.getName()); nodetypeVersionIter.hasNext();) {
                    Node version = nodetypeVersionIter.nextNode();
                    context.setName(version, "hipposysedit_1_0:nodetype");
                    if (convert && version.isNodeType("hippo:remodel")) {
                        if (uri.equals(version.getProperty("hippo:uri").getString())) {
                            current = version;
                        }
                    }
                    version.accept(new TraversingItemVisitor.Default(true) {
                        @Override
                        public void entering(final Node node, int level) throws RepositoryException {
                            convert(node, context);
                        }
                    });
                }
                context.setName(child, "hipposysedit_1_0:nodetype");
                context.setPrimaryNodeType(child, "hippo_2_0:handle");
                if (current != null) {
                    Node clone = ((HippoSession) child.getSession()).copy(current, current.getPath());
                    clone.setProperty("hipposysedit_1_0:uri", newUri);
                }

                if (node.hasNode("hippo:prototype")) {
                    child = node.getNode("hippo:prototype");
                    for (NodeIterator prototypeIter = child.getNodes("hippo:prototype"); prototypeIter.hasNext();) {
                        Node prototype = prototypeIter.nextNode();
                        context.setName(prototype, "hipposysedit_1_0:prototype");
                        prototype.accept(new TraversingItemVisitor.Default(true) {
                            @Override
                            public void entering(final Node node, int level) throws RepositoryException {
                                convert(node, context);
                            }
                        });
                    }
                    context.setName(child, "hipposysedit_1_0:prototypes");
                    context.setPrimaryNodeType(child, "hipposysedit_1_0:prototypeset");
                }

                if (node.hasNode("hippo:template")) {
                    node.addMixin("editor_1_0:editable");
                    child = node.getNode("hippo:template");
                    for (NodeIterator templateIter = child.getNodes(child.getName()); templateIter.hasNext();) {
                        Node template = templateIter.nextNode();
                        context.setName(template, "hipposysedit_1_0:template");
                        template.accept(new TraversingItemVisitor.Default(true) {
                            @Override
                            public void entering(final Node node, int level) throws RepositoryException {
                                convert(node, context);
                            }
                        });
                    }
                    context.setName(child, "editor_1_0:templates");
                    context.setPrimaryNodeType(child, "editor_1_0:templateset");
                }
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:configuration") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                for (String[] delete : new String[][] {
                            {"hippo:derivatives", "hippo:corederivatives"},
                            {"hippo:temporary"}, // this removal is appropriate, any changes to this folder should be considered transient
                            {"hippo:documents", "embedded", "root"},
                            {"hippo:queries"},
                            {"hippo:workflows"},
                            {"hippo:initialize"},
                            {"hippo:frontend"},
                            {"hippo:roles", "admin"},
                            {"hippo:groups", "admin"},
                            {"hippo:users", "admin", "workflowuser"}
                        }) {
                    for (NodeIterator it = node.getNode(delete[0]).getNodes(); it.hasNext();) {
                        Node child = it.nextNode();
                        boolean keep = false;
                        for (int i = 1; i < delete.length; i++) {
                            if (child.getName().equals(delete[i]))
                                keep = true;
                        }
                        if (!keep)
                            child.remove();
                    }
                }
                node.accept(new TraversingItemVisitor.Default(true) {
                    @Override
                    public void entering(final Node node, int level) throws RepositoryException {
                        convert(node, context);
                    }
                });
            }
        });
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            public void entering(final Node node, int level) throws RepositoryException {
                node.getNode("hippo:namespaces/hippo").remove();
                node.getNode("hippo:namespaces/system").remove();
                node.getNode("hippo:namespaces/hippostd").remove();
                node.getNode("hippo:namespaces/hippogallery").remove();
            }
        });
        for (String[] nodeTypeDefinitions : new String[][] {
                    {"hipposys"},
                    {"hipposysedit"},
                    {"hippo", "repository.cnd", "org.hippoecm.repository.LocalHippoRepository" },
                    {"hippostd"},
                    {"hippostd", "hippostd-addendum.cnd"},
                    {"hippogallery"},
                    {"frontend"},
                    {"hippolog"},
                    {"reporting"},
                    {"hippohtmlcleaner"},
                    {"editor"}}) {
            try {
                String prefix = nodeTypeDefinitions[0];
                String cndName = (nodeTypeDefinitions.length > 1 && nodeTypeDefinitions[1] != null ? nodeTypeDefinitions[1] : prefix + ".cnd");
                String classContext = (nodeTypeDefinitions.length > 2 && nodeTypeDefinitions[2] != null ? nodeTypeDefinitions[2] : null);
                InputStream cndStream;
                if (classContext != null) {
                    cndStream = Class.forName(classContext).getResourceAsStream(cndName);
                } else {
                    cndStream = getClass().getClassLoader().getResourceAsStream(cndName);
                }
                if (cndStream != null) {
                    context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, prefix, cndName, new InputStreamReader(cndStream)));
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace(System.err);
            }
        }

        try {
            Workspace workspace = context.getWorkspace();
            for(String subtypedNamespace : subTypedNamespaces(workspace)) {
                String uri = workspace.getNamespaceRegistry().getURI(subtypedNamespace);
                workspace.getNamespaceRegistry().registerNamespace(subtypedNamespace, VersionNumber.versionFromURI(uri).next().versionToURI(uri));
                String cnd = JcrCompactNodeTypeDefWriter.compactNodeTypeDef(workspace, subtypedNamespace);
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, subtypedNamespace, "-", new StringReader(cnd)));
            }
        } catch (NamespaceException ex) {
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    private Set<String> subTypedNamespaces(Workspace workspace) throws RepositoryException {
        Set<String> superTypeNamespaces = new HashSet<String>();
        Set<String> subtypedNamespaces = new HashSet<String>();
        Set<String> skippedNamespaces = new HashSet<String>();
        superTypeNamespaces.add("hippo");
        skippedNamespaces.add("hipposys");
        skippedNamespaces.add("hipposysedit");
        skippedNamespaces.add("hippostd");
        skippedNamespaces.add("hippogallery");
        skippedNamespaces.add("frontend");
        skippedNamespaces.add("hippolog");
        skippedNamespaces.add("reporting");
        skippedNamespaces.add("hippohtmlcleaner");
        skippedNamespaces.add("editor");
        skippedNamespaces.add("hipposched");
        skippedNamespaces.add("hippoldap");

        skippedNamespaces.addAll(superTypeNamespaces);
        NodeTypeManager ntMgr = workspace.getNodeTypeManager();
        boolean rerun;
        do {
            rerun = false;
            for (NodeTypeIterator ntiter = ntMgr.getAllNodeTypes(); ntiter.hasNext();) {
                NodeType nt = ntiter.nextNodeType();
                String ntName = nt.getName();
                if (ntName.contains(":")) {
                    String ntNamespace = ntName.substring(0, ntName.indexOf(":"));
                    if (!superTypeNamespaces.contains(ntNamespace) && !subtypedNamespaces.contains(ntNamespace)) {
                        for (NodeType superType : nt.getSupertypes()) {
                            String superName = superType.getName();
                            if (superName.contains(":")) {
                                String superNamespace = superName.substring(0, superName.indexOf(":"));
                                if (superTypeNamespaces.contains(superNamespace)) {
                                    superTypeNamespaces.add(ntNamespace);
                                    if (!skippedNamespaces.contains(ntNamespace))
                                        subtypedNamespaces.add(ntNamespace);
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

    private void convert(Node node, UpdaterContext context) throws RepositoryException {
        for (int i = 0; i < rules.length; i++) {
            String[] rule = rules[i];
            if ("type".equals(rule[0])) {
                boolean typeMatch = false;
                int typeMatchIndex = 0;
                for (NodeType nodeType : context.getNodeTypes(node)) {
                    if (nodeType.getName().equals(rule[1])) {
                        typeMatch = true;
                        if (typeMatchIndex == 0) {
                            if (rule[2] != null) {
                                context.setPrimaryNodeType(node, rule[2]);
                            } else {
                                node.remove();
                                return;
                            }
                        } else {
                            node.removeMixin(rule[1]);
                            node.addMixin(rule[2]);
                        }
                        break;
                    }
                    ++typeMatchIndex;
                }
                if (typeMatch) {
                    int j;
                    for (j = i + 1; j < rules.length; j++) {
                        rule = rules[j];
                        if ("field".equals(rule[0])) {
                            if (node.hasProperty(rule[1])) {
                                if (rule[2] != null) {
                                    if (context.isMultiple(node.getProperty(rule[1]))) {
                                        node.setProperty(rule[2], node.getProperty(rule[1]).getValues());
                                    } else {
                                        // TODO: whenever property is a Name (or Path), see if there is a prefix
                                        // in there that we can remap.
                                        if (rule[2].equals("hipposys_1_0:value")) {
                                            String value = node.getProperty(rule[1]).getString();
                                            for (int k = 0; k < rules.length; k++) {
                                                if (rules[k][0].equals("type") && rules[k][1].equals(value)) {
                                                    value = rules[k][2];
                                                    // hipposys:value is a String property, so the prefix will not be remapped
                                                    String prefix = value.substring(0, value.indexOf('_'));
                                                    value = prefix + value.substring(value.indexOf(':'));
                                                    break;
                                                }
                                            }
                                            node.setProperty(rule[2], value);
                                        } else {
                                            node.setProperty(rule[2], node.getProperty(rule[1]).getValue());
                                        }
                                    }
                                }
                                node.getProperty(rule[1]).remove();
                            }
                        } else if ("child".equals(rule[0])) {
                            for (NodeIterator iter = node.getNodes(rule[1]); iter.hasNext();) {
                                Node child = iter.nextNode();
                                if (rule[2] != null) {
                                    context.setName(child, rule[2]);
                                    if (rule.length > 3) {
                                        context.setPrimaryNodeType(child, rule[3]);
                                    }
                                } else {
                                    child.remove();
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    i = j - 1;
                } else {
                    int j;
                    for (j = i + 1; j < rules.length; j++) {
                        rule = rules[j];
                        if (!"field".equals(rule[0]) && !"child".equals(rule[0])) {
                            break;
                        }
                    }
                    i = j - 1;
                }
            } else if ("field".equals(rule[0])) {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            } else if ("child".equals(rule[0])) {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            } else {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            }
        }
    }

}
