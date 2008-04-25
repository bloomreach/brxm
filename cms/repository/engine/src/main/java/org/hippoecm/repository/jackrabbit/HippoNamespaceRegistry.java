/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import java.util.HashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoNamespaceRegistry extends NamespaceRegistryImpl {

    private static final Logger log = LoggerFactory.getLogger(HippoNamespaceRegistry.class);

    private HashMap<String, String> uriToPrefix = new HashMap<String, String>();
    private HashMap<String, String> prefixToUri = new HashMap<String, String>();
    private boolean open = false;

    protected HippoNamespaceRegistry(FileSystem nsRegStore) throws RepositoryException {
        super(nsRegStore);
    }

    public void open() {
        if (!open) {
            open = true;
        } else {
            log.warn("Registry was already opened for temporary changes");
        }
    }

    public void close() {
        if (open) {
            open = false;
        } else {
            log.warn("Registry wasn't open");
        }
    }

    public void commit(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException,
            AccessDeniedException, RepositoryException {
        if (prefixToUri.containsKey(prefix)) {
            String uri = prefixToUri.get(prefix);
            uriToPrefix.remove(uri);
            prefixToUri.remove(prefix);

            super.registerNamespace(prefix, uri);
        } else {
            log.warn("Unknown prefix " + prefix);
        }
    }

    @Override
    public synchronized void registerNamespace(String prefix, String uri) throws NamespaceException,
            UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
        if (open) {
            prefixToUri.put(prefix, uri);
            uriToPrefix.put(uri, prefix);
        } else {
            super.registerNamespace(prefix, uri);
        }
    }

    @Override
    public void unregisterNamespace(String prefix) {
        if (prefixToUri.containsKey(prefix)) {
            String uri = prefixToUri.get(prefix);
            uriToPrefix.remove(uri);
            prefixToUri.remove(prefix);
        } else {
            log.warn("Unknown prefix " + prefix);
        }
    }

    @Override
    public String[] getPrefixes() throws RepositoryException {
        String[] existing = super.getPrefixes();
        String[] result = new String[existing.length + prefixToUri.keySet().size()];
        prefixToUri.keySet().toArray(result);
        int i = prefixToUri.size();
        for (String prefix : existing) {
            result[i++] = prefix;
        }
        return result;
    }

    @Override
    public String[] getURIs() throws RepositoryException {
        String[] existing = super.getURIs();
        String[] result = new String[existing.length + uriToPrefix.keySet().size()];
        uriToPrefix.keySet().toArray(result);
        int i = prefixToUri.size();
        for (String uri : existing) {
            result[i++] = uri;
        }
        return result;
    }

    @Override
    public String getPrefix(String uri) throws NamespaceException {
        if (uriToPrefix.containsKey(uri)) {
            return uriToPrefix.get(uri);
        }
        return super.getPrefix(uri);
    }

    @Override
    public String getURI(String prefix) throws NamespaceException {
        if (prefixToUri.containsKey(prefix)) {
            return prefixToUri.get(prefix);
        }
        return super.getURI(prefix);
    }
}
