/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest.picker;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlTransient;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id$"
 */
public class JcrQuery implements Restful {


    private static final long serialVersionUID = 1L;

    public static final Set<String> EXCLUDED_FOLDER_PATHS = new ImmutableSet.Builder<String>()
            .add("/content/attic")
            .build();
    private boolean folderPicker = true;
    private boolean documentPicker;
    private boolean fetchProperties;
    private String path;
    private int depth;
    private int currentDepth;
    private Set<String> excludedPaths;
    private Set<String> documentTypes;

    public JcrQuery() {
    }


    public JcrQuery(final String path) {
        this.path = path;
    }


    public Set<String> getDocumentTypes() {
        return documentTypes;
    }

    public void setDocumentTypes(final Set<String> documentTypes) {
        this.documentTypes = documentTypes;
    }

    public Set<String> getExcludedPaths() {
        if (excludedPaths == null) {
            excludedPaths = new HashSet<>();
        }
        return excludedPaths;
    }

    public void setExcludedPaths(final Set<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @XmlTransient
    public int getCurrentDepth() {
        return currentDepth;
    }

    public void setCurrentDepth(final int currentDepth) {
        this.currentDepth = currentDepth;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(final int depth) {
        this.depth = depth;
    }

    public boolean isFolderPicker() {
        return folderPicker;
    }

    public void setFolderPicker(final boolean folderPicker) {
        this.folderPicker = folderPicker;
    }

    public boolean isDocumentPicker() {
        return documentPicker;
    }

    public void setDocumentPicker(final boolean documentPicker) {
        this.documentPicker = documentPicker;
    }

    public boolean isFetchProperties() {
        return fetchProperties;
    }

    public void setFetchProperties(final boolean fetchProperties) {
        this.fetchProperties = fetchProperties;
    }

    public void incrementDepth() {
        ++currentDepth;
    }

    //############################################
    // UTILS
    //############################################
    public boolean isExcluded(final Item node) throws RepositoryException {
        final String myPath = node.getPath();
        final boolean excluded = EXCLUDED_FOLDER_PATHS.contains(myPath);
        return excluded || getExcludedPaths().contains(myPath);

    }

    public boolean isOurDocument(final Node node) throws RepositoryException {
        return (documentTypes == null || documentTypes.contains(node.getPrimaryNodeType().getName()));
    }


}
