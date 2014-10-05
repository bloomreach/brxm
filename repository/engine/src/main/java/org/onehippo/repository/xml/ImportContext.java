/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.Node;

public class ImportContext {

    private final String parentAbsPath;
    private final InputStream inputStream;
    private final int uuidBehaviour;
    private final int referenceBehaviour;
    private final ContentResourceLoader contentResourceLoader;
    private final String startPath;
    private final Collection<String> contextPaths = new ArrayList<>();
    private Node baseNode;

    public ImportContext(final String parentAbsPath, final InputStream inputStream,
                         final int uuidBehaviour, final int referenceBehaviour,
                         final ContentResourceLoader contentResourceLoader,
                         final String startPath) {
        this.parentAbsPath = parentAbsPath;
        this.inputStream = inputStream;
        this.uuidBehaviour = uuidBehaviour;
        this.referenceBehaviour = referenceBehaviour;
        this.contentResourceLoader = contentResourceLoader;
        this.startPath = startPath;
    }

    public String getParentAbsPath() {
        return parentAbsPath;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public int getUuidBehaviour() {
        return uuidBehaviour;
    }

    public int getReferenceBehaviour() {
        return referenceBehaviour;
    }

    public ContentResourceLoader getContentResourceLoader() {
        return contentResourceLoader;
    }

    public String getStartPath() {
        return startPath;
    }

    public ImportResultImpl getImportResult() {
        return new ImportResultImpl();
    }

    void setBaseNode(Node baseNode) {
        this.baseNode = baseNode;
    }

    void addContextPath(String contextPath) {
        contextPaths.add(contextPath);
    }

    private class ImportResultImpl implements ImportResult {

        @Override
        public Collection<String> getContextPaths() {
            return Collections.unmodifiableCollection(contextPaths);
        }

        @Override
        public Node getBaseNode() {
            return baseNode;
        }
    }
}
