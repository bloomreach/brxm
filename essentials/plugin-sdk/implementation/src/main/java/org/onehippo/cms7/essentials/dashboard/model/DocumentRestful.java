/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "document")
public class DocumentRestful implements Restful {

    @XmlTransient
    private static final Logger log = LoggerFactory.getLogger(DocumentRestful.class);
    private static final long serialVersionUID = 1L;

    private String javaName;
    private String fullPath;
    private String fullName;
    private String name;
    private String prefix;
    private boolean mixin;
    private boolean draftMode;
    private boolean compoundType;
    private Set<String> superTypes;
    private Set<String> fieldLocations;

    public DocumentRestful(final ContentType contentType, final Session session, final PluginContext context) {
        this.fullName = extractFullName(contentType.getName(), context);
        this.prefix = extractPrefix(contentType.getPrefix(), fullName);
        this.mixin = contentType.isMixin();
        this.compoundType = contentType.isCompoundType();
        this.superTypes = contentType.getSuperTypes();
        this.name = extractName(fullName);
        setDraftType(session, prefix, name);
    }

    /**
     * Scheck if document is in draft mode:
     */
    private void setDraftType(final Session session, final String prefix, final String name) {

        try {
            final String absPath = "/hippo:namespaces/" + prefix + '/' + name + "/hipposysedit:prototypes";
            if (session.nodeExists(absPath)) {
                final Node node = session.getNode(absPath);
                final long size = node.getNodes().getSize();
                if (size > 1) {
                    // it holds more than one prototype (2):
                    draftMode = true;
                }
            }
        } catch (RepositoryException e) {
            log.error("Error checking draft node", e);
        }
    }

    private String extractPrefix(final String prefix, final String name) {
        if (!Strings.isNullOrEmpty(prefix)) {
            return prefix;
        }
        if (Strings.isNullOrEmpty(name)) {
            return null;
        }
        final int idx = name.indexOf(':');
        if (idx != -1) {
            return name.substring(0, idx);
        }
        return name;
    }

    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(final String javaName) {
        this.javaName = javaName;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    /**
     * TODO: change once https://issues.onehippo.com/browse/CMS7-8249 is fixed
     * Extract project name first. For a project with {@code testproject} namespace,
     * ContentService can return something  like {@code [hippostd:taggable, testproject:mydocument]} so we want second name to be chosen.
     *
     * @param name    name returned by content type service
     * @param context plugin context
     * @return
     */
    private String extractFullName(final CharSequence name, final PluginContext context) {
        final Iterable<String> split = Splitter.on(",").split(name);
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();
        for (String aName : split) {
            if (aName.startsWith(projectNamespacePrefix)) {
                return aName;
            }
        }

        return split.iterator().next();
    }

    public Set<String> getFieldLocations() {
        return fieldLocations;
    }

    public void setFieldLocations(final Set<String> fieldLocations) {
        this.fieldLocations = fieldLocations;
    }

    private String extractName(final String name) {
        final int idx = name.indexOf(':');
        if (idx != -1) {
            return name.substring((idx + 1), name.length());
        }
        return name;
    }


    public DocumentRestful() {
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public boolean isMixin() {
        return mixin;
    }

    public void setMixin(final boolean mixin) {
        this.mixin = mixin;
    }

    public boolean isCompoundType() {
        return compoundType;
    }

    public void setCompoundType(final boolean compoundType) {
        this.compoundType = compoundType;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(final Set<String> superTypes) {
        this.superTypes = superTypes;
    }


    public boolean isDraftMode() {
        return draftMode;
    }

    public void setDraftMode(final boolean draftMode) {
        this.draftMode = draftMode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocumentRestful{");
        sb.append("fullName='").append(fullName).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append(", mixin=").append(mixin);
        sb.append(", compoundType=").append(compoundType);
        sb.append(", superTypes=").append(superTypes);
        sb.append('}');
        return sb.toString();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }
}
