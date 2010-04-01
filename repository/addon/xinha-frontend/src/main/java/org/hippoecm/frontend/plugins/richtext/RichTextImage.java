/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.richtext;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * This class represents an image resource that a rich text field has a reference to.
 * It handles url generation and maintains the facetname attribute.
 */
public class RichTextImage implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String parentPath;
    private String path;
    private String name;
    private List<String> resourceDefinitions;
    private String selectedResourceDefinition;

    public RichTextImage(String targetPath, String name, String nodePath) {
        this.path = targetPath;
        this.name = name;
        this.parentPath = nodePath;
        this.resourceDefinitions = new ArrayList<String>();
    }

    public List<String> getResourceDefinitions() {
        return resourceDefinitions;
    }

    public void setResourceDefinitions(List<String> resourceDefinitions) {
        this.resourceDefinitions = resourceDefinitions;
        if (resourceDefinitions.size() == 0) {
            this.selectedResourceDefinition = null;
        } else if (!resourceDefinitions.contains(selectedResourceDefinition)) {
            selectedResourceDefinition = resourceDefinitions.get(0);
        }
    }

    public String getSelectedResourceDefinition() {
        return selectedResourceDefinition;
    }

    public void setSelectedResourceDefinition(String selectedResourceDefinition) {
        this.selectedResourceDefinition = selectedResourceDefinition;
    }

    public void setName(String facet) {
        this.name = facet;
    }

    public String getName() {
        return name;
    }

    public String getFacetSelectPath() {
        if (selectedResourceDefinition != null) {
            return name + "/{_document}/" + selectedResourceDefinition;
        } else {
            return name;
        }
    }

    public String getUrl() {
        String url = null;
        String parentUrl = "binaries" + parentPath + "/";

        if (!RichTextUtil.isPortletContext()) {
            if (selectedResourceDefinition != null) {
                url = RichTextUtil.encode(parentUrl + name + "/{_document}/" + selectedResourceDefinition);
            } else {
                url = RichTextUtil.encode(parentUrl + name);
            }
        } else {
            parentUrl = RichTextUtil.encodeResourceURL(RichTextUtil.encode(parentUrl));
            url = new StringBuilder(80).append(parentUrl).append(parentUrl.indexOf('?') == -1 ? '?' : '&').append(
                    "_path=").append(getFacetSelectPath()).toString();
        }

        return url;
    }

    public boolean isValid() {
        return path != null && !(getResourceDefinitions().size() > 1 && selectedResourceDefinition == null);
    }

    public IDetachable getTarget() {
        return new JcrNodeModel(path).getParentModel();
    }

}
