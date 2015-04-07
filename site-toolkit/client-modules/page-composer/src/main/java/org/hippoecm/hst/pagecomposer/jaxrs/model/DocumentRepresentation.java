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
    private boolean selected;

    // constructor for deserialization
    public DocumentRepresentation(){
    }

    public DocumentRepresentation(final String path,
                                  final String rootMountContentPath,
                                  final String displayName,
                                  final boolean isDocument,
                                  final boolean exists) {
            this.rootMountContentPath = rootMountContentPath;
            this.path = path;
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

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
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
        return result;
    }

    @Override
    public String toString() {
        return "DocumentRepresentation{" +
                "path='" + path + '\'' +
                ", rootMountContentPath='" + rootMountContentPath + '\'' +
                ", displayName='" + displayName + '\'' +
                ", isDocument=" + isDocument +
                ", exists=" + exists +
                ", selected=" + selected +
                '}';
    }
}
