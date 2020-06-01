/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Optional;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Values are automatically loaded by the cache, and are stored in the cache until either evicted or manually invalidated.
 */
public class TemplateLoadingCache {

    private static final Logger log = LoggerFactory.getLogger(TemplateLoadingCache.class);

    private final static String FREEMARKER_TMPL_BINARY_MIMETYPE = "application/octet-stream";
    private final static String HTML_TMPL_MIMETYPE = "text/html";

    private Map<String, Optional<RepositorySource>> cache =  new ConcurrentHashMap<>();

    private Repository repository;
    private Credentials credentials;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Returns the value associated with {@code key} in this cache, first loading that value if
     * necessary. This method is thread-safe
     */
    public RepositorySource get(String absPath) {
        Optional<RepositorySource> optional = cache.get(absPath);
        if (optional != null) {
            log.info("Return previously loaded repository source for '{}' (even if was null)", absPath);
            return optional.orNull();
        }
        log.info("Trying to load repository source for '{}'", absPath);
        synchronized (this) {
            optional = cache.get(absPath);
            if (optional != null) {
                return optional.orNull();
            }
            final RepositorySource repositoryTemplate = getRepositoryTemplate(absPath);
            cache.put(absPath, Optional.fromNullable(repositoryTemplate));
            return repositoryTemplate;
        }
    }

    public void remove(final String absPath) {
        cache.remove(absPath);
    }

    public void clear() {
        cache.clear();
    }

    private RepositorySource getRepositoryTemplate(String absPath) {
        Session session = null;
        try {
            session = createSession();
            if (session.itemExists(absPath)) {
                Item item = session.getItem(absPath);
                if (item.isNode()) {
                    Node templateNode = (Node) item;
                    if (templateNode.isNodeType(JcrConstants.NT_FILE)) {
                        return createRepositorySourceFromBinary(templateNode, absPath);
                    } else {
                        return createRepositorySource(
                                JcrUtils.getStringProperty(templateNode, HstNodeTypes.TEMPLATE_PROPERTY_SCRIPT, null),
                                absPath);
                    }
                } else {
                    return createRepositorySource(((Property) item).getValue().getString(), absPath);
                }
            } else {
                log.warn("No jcr node or property found at '{}'. Cannot return freemarker template.", absPath);
                return RepositorySource.notFound(absPath);
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException while fetching freemarker template from repository", e);
            return RepositorySource.notFound(absPath);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    private Session createSession() throws RepositoryException {
        return repository.login(credentials);
    }

    private RepositorySource createRepositorySourceFromBinary(final Node templateNode, final String absPath) throws RepositoryException {
        final Node content = templateNode.getNode(JcrConstants.JCR_CONTENT);
        String mimeType = JcrUtils.getStringProperty(content, JcrConstants.JCR_MIME_TYPE, null);
        if (!FREEMARKER_TMPL_BINARY_MIMETYPE.equals(mimeType) && !HTML_TMPL_MIMETYPE.equals(mimeType)) {
            log.warn("Expected freemarker binary or HTML at '{}' with mimetype '{}' or '{}' but was '{}'. Cannot return " +
                    "ftl for wrong mimetype", absPath, FREEMARKER_TMPL_BINARY_MIMETYPE, HTML_TMPL_MIMETYPE, mimeType);
            return RepositorySource.notFound(absPath);
        }
        final Binary ftl = JcrUtils.getBinaryProperty(content, JcrConstants.JCR_DATA, null);
        if (ftl == null) {
            log.warn("Expected freemarker binary at '{}' but binary was null. Cannot return " +
                    "ftl for wrong mimetype", absPath);
            return RepositorySource.notFound(absPath);
        }

        final InputStream stream = ftl.getStream();
        try {
            try {
                String template = IOUtils.toString(stream, "UTF-8");
                return createRepositorySource(template, absPath);
            } catch (IOException e) {
                log.warn("Exception while reading freemarker binary from '{}'", absPath, e);
                return RepositorySource.notFound(absPath);
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    private RepositorySource createRepositorySource(String template, String absJcrPath) {
        if (template == null) {
            log.debug("Template source '{}' not found in the repository. ", absJcrPath);
            return RepositorySource.notFound(absJcrPath);
        }
        return RepositorySource.found(absJcrPath, template);
    }

}
