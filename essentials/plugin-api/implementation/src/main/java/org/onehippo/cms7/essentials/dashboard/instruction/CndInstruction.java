/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.instruction;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.ArrayUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.InstructionEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;


/**
 * @version "$Id$"
 */
@Component
@XmlRootElement(name = "cnd", namespace = EssentialConst.URI_ESSENTIALS_INSTRUCTIONS)
public class CndInstruction extends PluginInstruction {

    //private static final String DEFAULT_URL = "http://www.onehippo.org/${namespace}/nt/1.0";
    private static Logger log = LoggerFactory.getLogger(CndInstruction.class);
    @Inject
    private EventBus eventBus;
    // TODO implement possibility to add custom namespace, currently only project namespaces is used
    private String namespace;
    private String superType;
    private String documentType;

    @Named("${instruction.message.cnd.register.failed}")
    private String messageRegisterError;

    @Named("${instruction.message.cnd.register}")
    private String message;
    private String action;

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {

        namespace = context.getProjectNamespacePrefix();
        final Map<String, Object> data = context.getPlaceholderData();
        processAllPlaceholders(data);
        //


        final String prefix = context.getProjectNamespacePrefix();
        try {
            // TODO extend so we can define supertypes
            String[] superTypes;
            if (!Strings.isNullOrEmpty(superType)) {
                final Iterable<String> split = Splitter.on(",").omitEmptyStrings().trimResults().split(superType);
                superTypes = Iterables.toArray(split, String.class);
            } else {
                superTypes = ArrayUtils.EMPTY_STRING_ARRAY;
            }
            CndUtils.registerDocumentType(context, prefix, documentType, true, false, superTypes);
            context.createSession().save();
            // TODO add message
            eventBus.post(new InstructionEvent(this));
            return InstructionStatus.SUCCESS;
        } catch (NodeTypeExistsException e) {
            // just add already exiting ones:
            GlobalUtils.refreshSession(context.createSession(), false);

        } catch (RepositoryException e) {
            log.error(String.format("Error registering document type: %s", namespace), e);
            GlobalUtils.refreshSession(context.createSession(), false);
        }

        message = messageRegisterError;
        eventBus.post(new InstructionEvent(this));
        return InstructionStatus.FAILED;
    }

    private void processAllPlaceholders(final Map<String, Object> data) {
        data.put("documentType", documentType);
        data.put("superType", superType);
        processPlaceholders(data);
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
        final String myErrorMessage = TemplateUtils.replaceTemplateData(messageRegisterError, data);
        if (myErrorMessage != null) {
            messageRegisterError = myErrorMessage;
        }
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(final String message) {
        this.message = message;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(final String action) {
        this.action = action;
    }

    @XmlAttribute
    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
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
