/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.JcrConstants;
import org.onehippo.cm.model.impl.SourceLocationImpl;
import org.xml.sax.SAXException;

public class EsvNode {

    private String name;
    private final int index;
    private final SourceLocationImpl location;
    private EsvMerge merge;
    private String mergeLocation;
    private List<EsvProperty> properties = new ArrayList<>();
    private List<EsvNode> children = new ArrayList<>();
    private boolean migrationFixed;

    private final String AUTH_ROLE_NT = "hipposys:authrole";
    private final String HIPPOSYS_ROLE = "hipposys:role";
    private final String EFORMS_VALIDATIONRULE = "eforms:validationrule";
    private final String EFORMS_DATERULE = "eforms:daterule";
    private final String EFORMS_VALIDATIONRULEID = "eforms:validationruleid";
    private final String EFORMS_DATERULEID = "eforms:dateruleid";

    public EsvNode(final String name, final int index, SourceLocationImpl location) {
        this.name = name;
        this.index = index;
        this.location = location;
    }

    void migrationFixes() throws SAXException {
        if (!migrationFixed) {
            if (AUTH_ROLE_NT.equals(getType())) {
                EsvProperty prop = getProperty(HIPPOSYS_ROLE);
                if (prop != null && prop.getValue() != null) {
                    name = prop.getValue();
                }
            }
            else if (EFORMS_VALIDATIONRULE.equals(name)) {
                EsvProperty prop = getProperty(EFORMS_VALIDATIONRULEID);
                if (prop != null) {
                    name = prop.getValue();
                    properties.remove(prop);
                } else {
                    throw new SAXException("Incomplete '"+EFORMS_VALIDATIONRULE+"' node definition: missing required property '"
                            +EFORMS_VALIDATIONRULEID+"' at "+getSourceLocation());
                }
            }
            else if (EFORMS_DATERULE.equals(name)) {
                EsvProperty prop = getProperty(EFORMS_DATERULEID);
                if (prop != null) {
                    name = prop.getValue();
                    properties.remove(prop);
                } else {
                    throw new SAXException("Incomplete '" + EFORMS_DATERULE + "' node definition: missing required property '"
                            + EFORMS_DATERULEID + "' at " + getSourceLocation());
                }
            }
            migrationFixed = true;
        }
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public SourceLocationImpl getSourceLocation() {
        return location;
    }

    public String getType() {
        EsvProperty prop = getProperty(JcrConstants.JCR_PRIMARYTYPE);
        return prop != null ? prop.getValue() : null;
    }

    public EsvMerge getMerge() {
        return merge;
    }

    public void setMerge(final EsvMerge merge) {
        this.merge = merge;
    }

    public boolean isDeltaCombine() {
        return EsvMerge.COMBINE == merge;
    }

    public boolean isDeltaMerge() {
        return isDeltaCombine() || isDeltaOverlay();
    }

    public boolean isDeltaSkip() {
        return EsvMerge.SKIP == merge;
    }

    public boolean isDeltaOverlay() {
        return EsvMerge.OVERLAY == merge;
    }

    public boolean isDeltaInsert() {
        return EsvMerge.INSERT == merge;
    }

    public String getMergeLocation() {
        return mergeLocation != null ? mergeLocation : "";
    }

    public void setMergeLocation(final String mergeLocation) {
        this.mergeLocation = mergeLocation;
    }

    public List<EsvProperty> getProperties() {
        return properties;
    }

    public EsvProperty getProperty(final String name) {
        for (EsvProperty prop : properties) {
            if (prop.getName().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    public List<EsvNode> getChildren() {
        return children;
    }
}
