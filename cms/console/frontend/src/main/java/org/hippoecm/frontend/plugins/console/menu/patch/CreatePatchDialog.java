/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.patch;


import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.MultiStepDialog;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.LoadInitializationModule;
import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.decorating.WorkspaceDecorator;
import org.hippoecm.repository.impl.DecoratorFactoryImpl;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.onehippo.cms7.jcrdiff.JcrDiffException;
import org.onehippo.cms7.jcrdiff.content.jcr.JcrTreeNode;
import org.onehippo.cms7.jcrdiff.delta.Patch;
import org.onehippo.cms7.jcrdiff.match.Matcher;
import org.onehippo.cms7.jcrdiff.match.MatcherItemInfo;
import org.onehippo.cms7.jcrdiff.match.PatchFactory;
import org.xml.sax.InputSource;

public class CreatePatchDialog extends MultiStepDialog {

    private static final String[] ILLEGAL_PATHS = new String[] {
            "/content",
            "/hippo:log",
            "/formdata",
            "/jcr:system"
    };

    private final Label diff;
    private String path;
    private Step[] steps;

    public CreatePatchDialog(IModel<Node> model) {
        super(model);
        diff = new Label("diff");
        diff.setOutputMarkupId(true);
        add(diff);
        try {
            path = getModelObject().getPath();
            if (!isPathValid(path)) {
                error("Creating a diff for path " + path + " is not supported");
                setOkEnabled(false);
            }
        } catch (RepositoryException e) {
            setOkEnabled(false);
        }
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Create Diff for " + path);
    }

    @Override
    protected Step[] getSteps() {
        if (steps == null) {
            steps = new Step[] { new CreatePatchStep(), new DoneStep() };
        }
        return steps;
    }

    private boolean isPathValid(String path) {
        if (path.equals("/")) {
            return false;
        }
        for (String illegalPath : ILLEGAL_PATHS) {
            if (path.startsWith(illegalPath)) {
                return false;
            }
        }
        return true;
    }

    private String createDiff() {
        javax.jcr.Session referenceSession = null;
        try {
            String workspaceName = getWorkspaceHash();
            // create reference nodes
            referenceSession = getSessionForReferenceWorkspace(workspaceName);
            if (!referenceSession.nodeExists("/hippo:configuration")) {
                bootstrapReferenceWorkspace(referenceSession);
            }

            // check if reference node exists
            if (!referenceSession.nodeExists(path)) {
                error("No node at " + path + " in reference repository");
                return "";
            }
            final Patch patch = createPatch(getModelObject(), referenceSession.getNode(path));
            final JAXBContext context = JAXBContext.newInstance(Patch.class);
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            final StringWriter writer = new StringWriter();
            marshaller.marshal(patch, writer);
            return writer.getBuffer().toString();
        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (IOException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (JcrDiffException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (JAXBException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            error("An unexpected error occurred: " + e.getMessage());
        } finally {
            if (referenceSession != null) {
                referenceSession.logout();
            }
        }

        return null;
    }

    private void bootstrapReferenceWorkspace(javax.jcr.Session session) throws RepositoryException, IOException {

        // import the core configuration
        LoadInitializationModule.initializeNodecontent(session, "/", LocalHippoRepository.class.getResourceAsStream("configuration.xml"), "configuration.xml");
        session.save();

        // load all extension resources
        LoadInitializationModule.initializeExtensions(session, session.getNode("/hippo:configuration/hippo:initialize"));
        LoadInitializationModule.refresh(session, true);
    }


    private Patch createPatch(final Node currentNode, final Node referenceNode) throws JcrDiffException, JAXBException, IOException {

        final Matcher matcher = new Matcher();
        final MatcherItemInfo currentInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(currentNode));
        final MatcherItemInfo cleanInfo = new MatcherItemInfo(matcher.getContext(), new JcrTreeNode(referenceNode));
        matcher.setSource(cleanInfo);
        matcher.setResult(currentInfo);
        matcher.match();

        final PatchFactory factory = new PatchFactory();
        return factory.createPatch(cleanInfo, currentInfo);
    }

    private javax.jcr.Session getSessionForReferenceWorkspace(final String workspaceName) throws RepositoryException {
        final javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
        final RepositoryImpl repository = (RepositoryImpl) RepositoryDecorator.unwrap(session.getRepository());
        javax.jcr.Session rootSession;
        try {
            rootSession = repository.getRootSession(workspaceName);
        } catch (NoSuchWorkspaceException e) {
            final InputSource is = new InputSource(getClass().getResourceAsStream("workspace.xml"));
            ((JackrabbitWorkspace) WorkspaceDecorator.unwrap(session.getWorkspace())).createWorkspace(workspaceName, is);
            rootSession = repository.getRootSession(workspaceName);
        }
        return DecoratorFactoryImpl.getSessionDecorator(rootSession.impersonate(new SimpleCredentials("system", new char[]{})));
    }

    private String getWorkspaceHash() throws IOException, NoSuchAlgorithmException {
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (String jarName : getConfigJarNames()) {
            md5.update(jarName.getBytes());
        }
        final String s = DatatypeConverter.printBase64Binary(md5.digest());
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '+' || c == '/' || c == '=') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private List<String> getConfigJarNames() throws IOException {
        final List<String> extensions = new LinkedList<String>();
        Enumeration<URL> iter = Thread.currentThread().getContextClassLoader().getResources("org/hippoecm/repository/extension.xml");
        while (iter.hasMoreElements()) {
            final String fileName = getJarFileName(iter.nextElement().getFile());
            extensions.add(fileName);
        }
        iter = Thread.currentThread().getContextClassLoader().getResources("hippoecm-extension.xml");
        while (iter.hasMoreElements()) {
            final String fileName = getJarFileName(iter.nextElement().getFile());
            extensions.add(fileName);
        }
        Collections.sort(extensions);
        return extensions;
    }

    private String getJarFileName(final String file) {
        int offset = file.indexOf("!");
        if (offset != -1) {
            String jarFile = file.substring(0, offset);
            offset = jarFile.lastIndexOf('/');
            if (offset != -1) {
                return jarFile.substring(offset+1);
            }
        }
        return file;
    }

    private class CreatePatchStep extends Step {

        @Override
        protected int execute() {
            diff.setDefaultModel(new Model<String>(createDiff()));
            AjaxRequestTarget.get().addComponent(diff);
            return 1;
        }

        @Override
        protected IModel<String> getOkLabel() {
            return new Model<String>("Show Diff");
        }
    }
}
