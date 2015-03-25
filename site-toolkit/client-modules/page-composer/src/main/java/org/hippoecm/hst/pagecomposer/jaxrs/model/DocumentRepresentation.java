/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "document")
public class DocumentRepresentation {
    private String path;
    private String rootMountContentPath;
    private String displayName;
    private boolean isDocument;
    private boolean exists;

    // constructor for deserialization
    public DocumentRepresentation(){
    }

    public DocumentRepresentation(final String absPath,
                                  final String rootMountContentPath,
                                  final String displayName,
                                  final boolean isDocument,
                                  final boolean exists) {
        if (absPath == null || rootMountContentPath == null) {
            throw new IllegalArgumentException("absPath and rootMountContentPath are not allowed to be null");
        }
        if (!absPath.startsWith(rootMountContentPath + "/")) {
            throw new IllegalArgumentException("Path must start with rootMountContentPath");
        }
        this.rootMountContentPath = rootMountContentPath;
        path = absPath.substring(rootMountContentPath.length() + 1);
        this.displayName = displayName;
        this.isDocument = isDocument;
        this.exists = exists;
    }

    /**
     * @return the relative (to root channel content path) content path
     */
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public boolean isDocument() {
        return isDocument;
    }

    public void setDocument(final boolean isDocument) {
        this.isDocument = isDocument;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(final boolean exists) {
        this.exists = exists;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DocumentRepresentation that = (DocumentRepresentation) o;

        if (exists != that.exists) {
            return false;
        }
        if (isDocument != that.isDocument) {
            return false;
        }
        if (!path.equals(that.path)) {
            return false;
        }
        if (!rootMountContentPath.equals(that.rootMountContentPath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + rootMountContentPath.hashCode();
        result = 31 * result + (isDocument ? 1 : 0);
        result = 31 * result + (exists ? 1 : 0);
        return result;
    }
}
