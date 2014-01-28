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

package org.onehippo.cms7.essentials.rest.model.contentblocks;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ContentBlockModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ContentBlockModel.class);

    private String provider;
    private Type type;
    private String documentType;
    private Prefer prefer;
    private String name;

    public ContentBlockModel(final String provider, final Prefer prefer, final Type type, final String name, final String documentType) {
        this.provider = provider;
        this.type = type;
        this.documentType = documentType;
        this.prefer = prefer;
        this.name = name;
    }

    public ContentBlockModel(Node node) {
        try {
            final String myProvider = getProperty(node, "cpItemsPath").getString();
            final Node _default_ = node.getParent();
            final Property myDocumentType = getProperty(_default_, "type");
            final String myName = node.getName();
            setName(myName);
            setDocumentType(myDocumentType.getString());
            setProvider(myProvider);
        } catch (RepositoryException e) {
            log.error("Repository exception while trying to populate content blocks added list. Check if the property \"type\" exists on your _default_ node in you namespace template {}", e);
        }
    }

    private Property getProperty(final Node node, final String property) throws RepositoryException {
        if (node.hasProperty(property)) {
            return node.getProperty(property);
        }
        return null;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }

    public Prefer getPrefer() {
        return prefer;
    }

    public void setPrefer(final Prefer prefer) {
        this.prefer = prefer;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }


    public enum Prefer implements Serializable {
        LEFT("left"), RIGHT("right");
        private String prefer;

        private Prefer(String prefer) {
            this.prefer = prefer;
        }

        public String getPrefer() {
            return prefer;
        }
    }

    public enum PluginType {

        LISTVIEWPLUGIN("org.hippoecm.frontend.service.render.ListViewPlugin"), TWOCOLUMN("org.hippoecm.frontend.editor.layout.TwoColumn"), UNKNOWN("unknown");
        private String clazz;

        PluginType(String clazz) {
            this.clazz = clazz;
        }

        public static PluginType get(String clazz) {
            for (PluginType a : PluginType.values()) {
                if (a.clazz.equals(clazz)) {
                    return a;
                }
            }
            return UNKNOWN;
        }

        public String getClazz() {
            return clazz;
        }

    }
    /*
    public enum WicketId {
        LEFT("${cluster.id}.left.item"), RIGHT("${cluster.id}.right.item"), DEFAULT("${cluster.id}.field");
        private String wicketId;

        WicketId(final String wicketId) {
            this.wicketId = wicketId;
        }

        public static WicketId get(String id) {
            for (WicketId a : WicketId.values()) {
                if (a.getWicketId().equals(id)) {
                    return a;
                }
            }
            return DEFAULT;
        }

        private String getWicketId() {
            return wicketId;
        }

    }
    */

    public enum Type implements Serializable {
        LINKS("links"), DROPDOWN("dropdown");
        private String type;

        private Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ContentBlockModel");
        sb.append("{provider='").append(provider).append('\'');
        sb.append(", type=").append(type);
        sb.append(", documentType='").append(documentType).append('\'');
        sb.append(", prefer=").append(prefer);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
