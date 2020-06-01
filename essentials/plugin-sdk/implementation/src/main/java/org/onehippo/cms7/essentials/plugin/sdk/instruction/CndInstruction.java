/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.instruction;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import org.apache.commons.lang.ArrayUtils;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.CndUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "cnd", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class CndInstruction extends BuiltinInstruction {

    private static Logger log = LoggerFactory.getLogger(CndInstruction.class);
    // TODO implement possibility to add custom namespace, currently only project namespaces is used
    private String namespace;
    private String superType;
    private String documentType;
    private String namespacePrefix;

    @Inject private JcrService jcrService;
    @Inject private SettingsService settingsService;
    @Inject private PlaceholderService placeholderService;

    public CndInstruction() {
        super(Type.DOCUMENT_REGISTER);
    }

    @Override
    public Status execute(final Map<String, Object> parameters) {
        preProcessAttributes();

        final Session session = jcrService.createSession();
        try {
            // TODO extend so we can define supertypes
            String[] superTypes;
            if (!Strings.isNullOrEmpty(superType)) {
                final Iterable<String> split = Splitter.on(",").omitEmptyStrings().trimResults().split(superType);
                superTypes = Iterables.toArray(split, String.class);
            } else {
                superTypes = ArrayUtils.EMPTY_STRING_ARRAY;
            }
            CndUtils.registerDocumentType(jcrService, namespace, documentType, true, false, superTypes);
            session.save();
            log.info("Successfully registered document type '{}:{}'.", namespace, documentType);
        } catch (NodeTypeExistsException e) {
            log.info("Node type '{}:{}' already exists.", namespace, documentType);
            return Status.SKIPPED;
        } catch (RepositoryException e) {
            log.error("Error registering document type '{}:{}': ", namespace, documentType, e);
            return Status.FAILED;
        } finally {
            jcrService.destroySession(session);
        }
        return Status.SUCCESS;
    }

    @Override
    void populateDefaultChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(getDefaultGroup(), "Register document type '" + documentType + "'.");
    }

    private void preProcessAttributes() {
        if (Strings.isNullOrEmpty(namespacePrefix)) {
            namespace = settingsService.getSettings().getProjectNamespace();
        } else {
            namespace = namespacePrefix;
        }

        final Map<String, Object> data = placeholderService.makePlaceholders();
        final String mySupertype = TemplateUtils.replaceTemplateData(superType, data);
        if (mySupertype != null) {
            superType = mySupertype;
        }
        final String myNamespace = TemplateUtils.replaceTemplateData(namespace, data);
        if (myNamespace != null) {
            namespace = myNamespace;
        }
        final String myDocumentType = TemplateUtils.replaceTemplateData(documentType, data);
        if (myDocumentType != null) {
            documentType = myDocumentType;
        }
    }

    @XmlAttribute
    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }


    @XmlAttribute
    public String getNamespacePrefix() {
        return namespacePrefix;
    }

    public void setNamespacePrefix(final String namespacePrefix) {
        this.namespacePrefix = namespacePrefix;
    }

    @XmlAttribute
    public String getSuperType() {
        return superType;
    }

    public void setSuperType(final String superType) {
        this.superType = superType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CndInstruction{");
        sb.append("namespace='").append(namespace).append('\'');
        sb.append(", superType='").append(superType).append('\'');
        sb.append(", documentType='").append(documentType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
