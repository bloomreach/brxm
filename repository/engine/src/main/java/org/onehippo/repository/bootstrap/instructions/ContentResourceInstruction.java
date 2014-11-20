/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.lang.StringUtils;
import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;
import org.onehippo.repository.bootstrap.util.PartialSystemViewFilter;
import org.onehippo.repository.xml.DefaultContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.INIT_FOLDER_PATH;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.getNodeIndex;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.initializeNodecontent;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.removeNode;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.reorderNode;

public class ContentResourceInstruction extends InitializeInstruction {

    public ContentResourceInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_CONTENTRESOURCE;
    }

    @Override
    protected boolean canCombine(final InitializeInstruction instruction) {
        return instruction instanceof ContentDeleteInstruction;
    }

    public PostStartupTask execute() throws RepositoryException {
        final String contentResource = item.getContentResource();
        final URL contentURL = item.getContentResourceURL();
        if (contentURL == null) {
            throw new RepositoryException(String.format("Not found: %s", contentResource));
        }
        boolean pckg = contentResource.endsWith(".zip") || contentResource.endsWith(".jar");

        String contentRoot = item.getContentRoot();
        if (contentRoot.equals(INIT_FOLDER_PATH) || contentRoot.startsWith(INIT_FOLDER_PATH + "/")) {
            throw new RepositoryException(String.format("Bootstrapping content to %s is not supported", INIT_FOLDER_PATH));
        }
        if (item.isReloadable()) {
            final String contextNodePath = item.getContextPath();
            if (contextNodePath != null ) {
                final int index = getNodeIndex(session, contextNodePath);
                if (session.nodeExists(contextNodePath)) {
                    removeNode(session, contextNodePath, false);
                }
                initializeNodecontent(session, contentRoot, contentURL, pckg);
                if (index != -1) {
                    reorderNode(session, contextNodePath, index);
                }
            } else {
                throw new RepositoryException("Cannot reload item because context node could not be determined");
            }
        } else {
            InputStream in = null;
            try {
                if (item.isDownstreamItem()) {
                    Collection<String> processedPaths = new ArrayList<>();
                    for (InitializeItem upstreamItem : item.getUpstreamItems()) {
                        final String upstreamItemContextPath = upstreamItem.getContextPath();
                        if (upstreamItemContextPath == null) {
                            throw new RepositoryException("Unable to reload downstream item: can't determine upstream item context path");
                        }
                        if (partAlreadyApplied(processedPaths, upstreamItemContextPath)) {
                            continue;
                        }
                        processedPaths.add(upstreamItemContextPath);
                        in = contentURL.openStream();
                        final String upstreamItemContentRoot = upstreamItem.getContentRoot();
                        if (upstreamItemContentRoot.length() > contentRoot.length()) {
                            String contextRelPath = StringUtils.substringAfter(upstreamItemContextPath, contentRoot + "/");
                            contentRoot = upstreamItemContentRoot;
                            in = getPartialContentInputStream(in, contextRelPath);
                        }
                        initializeNodecontent(session, contentRoot, in, contentURL, pckg);
                        IOUtils.closeQuietly(in);
                    }
                } else {
                    initializeNodecontent(session, contentRoot, contentURL, pckg);
                }
            } catch (IOException e) {
                throw new RepositoryException("Failed to open content stream");
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
        return null;
    }

    private boolean partAlreadyApplied(final Collection<String> processedPaths, final String upstreamItemContextPath) {
        for (String processedPath : processedPaths) {
            if (upstreamItemContextPath.equals(processedPath)
                    || upstreamItemContextPath.startsWith(processedPath + "/")) {
                return true;
            }
        }
        return false;
    }

    InputStream getPartialContentInputStream(InputStream in, final String contextRelPath) throws IOException, RepositoryException {
        File file = File.createTempFile("bootstrap-", ".xml");
        OutputStream out = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            SAXParser parser = factory.newSAXParser();

            out = new FileOutputStream(file);
            TransformerHandler handler = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            handler.setResult(new StreamResult(out));

            parser.parse(new InputSource(in), new DefaultContentHandler(new PartialSystemViewFilter(handler, contextRelPath)));
            return new TempFileInputStream(file);
        } catch (FactoryConfigurationError e) {
            throw new RepositoryException("SAX parser implementation not available", e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException("SAX parser configuration error", e);
        } catch (SAXException e) {
            Exception exception = e.getException();
            if (exception instanceof RepositoryException) {
                throw (RepositoryException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new InvalidSerializedDataException("Error parsing XML import", e);
            }
        } catch (TransformerConfigurationException e) {
            throw new RepositoryException("SAX transformation error", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * An input stream from a temporary file. The file is deleted when the stream is
     * closed or garbage collected.
     */
    private static class TempFileInputStream extends FilterInputStream {

        private final File file;

        public TempFileInputStream(File file) throws FileNotFoundException {
            this(new FileInputStream(file), file);
        }

        protected TempFileInputStream(FileInputStream in, File file) {
            super(in);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            in.close();
            in = new ClosedInputStream();
            file.delete();
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }
    }
}
