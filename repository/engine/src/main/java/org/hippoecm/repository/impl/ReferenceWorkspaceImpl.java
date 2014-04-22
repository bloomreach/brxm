/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.xml.bind.DatatypeConverter;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.xml.sax.InputSource;

public class ReferenceWorkspaceImpl implements ReferenceWorkspace {

    private final RepositoryImpl repositoryImpl;
    private final String workspaceName;


    public ReferenceWorkspaceImpl(final RepositoryImpl repositoryImpl) throws RepositoryException {
        this.repositoryImpl = repositoryImpl;
        try {
            this.workspaceName = "REF" + getWorkspaceHash();
        } catch (NoSuchAlgorithmException e) {
            throw new RepositoryException("Failed to create reference workspace", e);
        } catch (IOException e) {
            throw new RepositoryException("Failed to create reference workspace", e);
        }
        createWorkspaceIfNotExists();
    }

    @Override
    public Session login() throws RepositoryException {
        final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
        return DecoratorFactoryImpl.getSessionDecorator(repositoryImpl.getRootSession(workspaceName).impersonate(credentials), credentials);
    }

    @Override
    public void bootstrap() throws RepositoryException, IOException {
        final Session session = login();
        try {
            final InitializationProcessorImpl initializationProcessor = new InitializationProcessorImpl();

            if (!session.nodeExists("/hippo:configuration")) {
                initializationProcessor.initializeNodecontent(session, "/", LocalHippoRepository.class.getResourceAsStream("configuration.xml"), null);
            }

            session.save();

            final List<Node> initializeItems = initializationProcessor.loadExtensions(session, session.getNode(InitializationProcessor.INITIALIZATION_FOLDER), false);
            final List<Node> contentItems = new ArrayList<Node>();

            for (Node initializeItem : initializeItems) {
                if (isContentInitializeItem(initializeItem) && isRelevantContentRoot(initializeItem)) {
                    contentItems.add(initializeItem);
                }
            }

            Collections.sort(contentItems, new Comparator<Node>() {
                @Override
                public int compare(final Node n1, final Node n2) {
                    try {
                        double s1 = n1.getProperty(HippoNodeType.HIPPO_SEQUENCE).getDouble();
                        double s2 = n2.getProperty(HippoNodeType.HIPPO_SEQUENCE).getDouble();
                        return s1 == s2 ? 0 : s1 < s2 ? -1 : 1;
                    } catch (RepositoryException e) {
                        return 0;
                    }
                }
            });

            initializationProcessor.processInitializeItems(session, contentItems);
        } finally {
            session.logout();
        }
    }

    @Override
    public void clean() throws RepositoryException {
        final Session session = login();
        try {
            final NodeIterator nodes = session.getRootNode().getNodes();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                if (!node.getName().startsWith("jcr:")) {
                    node.remove();
                }
            }
            session.save();
        } finally {
            session.logout();
        }
    }

    private void createWorkspaceIfNotExists() throws RepositoryException {
        try {
            repositoryImpl.getRootSession(workspaceName);
        } catch (NoSuchWorkspaceException e) {
            final InputSource is = new InputSource(getClass().getResourceAsStream("reference-workspace.xml"));
            ((JackrabbitWorkspace)repositoryImpl.getRootSession(null).getWorkspace()).createWorkspace(workspaceName, is);
        }
    }

    /**
     * We use a hash of all the content jar file names as the name of our workspace.
     * That way, we can create a reference workspace once for each version of a deployment and
     * we won't have to create and bootstrap the complete reference workspace every time we need it.
     */
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
        Enumeration<URL> enumeration = Thread.currentThread().getContextClassLoader().getResources("org/hippoecm/repository/extension.xml");
        while (enumeration.hasMoreElements()) {
            final String fileName = getJarFileName(enumeration.nextElement().getFile());
            extensions.add(fileName);
        }
        enumeration = Thread.currentThread().getContextClassLoader().getResources("hippoecm-extension.xml");
        while (enumeration.hasMoreElements()) {
            final String fileName = getJarFileName(enumeration.nextElement().getFile());
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

    private boolean isContentInitializeItem(Node initializeItem) throws RepositoryException {
        return initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE)
                || initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTDELETE)
                || initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENT)
                || initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTPROPSET)
                || initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTPROPADD);
    }

    private boolean isRelevantContentRoot(Node initializeItem) throws RepositoryException {
        if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTROOT)) {
            // since we don't expect diffs against content to be meaningful we exclude it to avoid overhead
            return !initializeItem.getProperty(HippoNodeType.HIPPO_CONTENTROOT).getString().startsWith("/content");
        }
        return true;
    }
}
