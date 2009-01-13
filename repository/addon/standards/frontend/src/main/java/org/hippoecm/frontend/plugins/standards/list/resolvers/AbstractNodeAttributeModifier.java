/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNodeAttributeModifier implements IListAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AbstractNodeAttributeModifier.class);

    public AttributeModifier[] getCellAttributeModifiers(IModel model) {
        if (model instanceof JcrNodeModel) {
            try {
                HippoNode node = (HippoNode) model.getObject();
                if (node != null) {
                    return getCellAttributeModifiers(node);
                } else {
                    log.warn("Cannot render a null node");
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return null;
    }

    protected AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
        return null;
    }

    protected AttributeModifier[] getCellAttributeModifiers(HippoNode node) throws RepositoryException {
        AttributeModifier modifier = getCellAttributeModifier(node);
        if(modifier != null) {
            return new AttributeModifier[] { modifier };
        } else {
            return null;
        }
    }

    public AttributeModifier[] getColumnAttributeModifiers(IModel model) {
        if (model instanceof JcrNodeModel) {
            try {
                HippoNode node = (HippoNode) model.getObject();
                if (node != null) {
                    return getColumnAttributeModifiers(node);
                } else {
                    log.warn("Cannot render a null node");
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return null;
    }

    protected AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return null;
    }

    protected AttributeModifier[] getColumnAttributeModifiers(HippoNode node) throws RepositoryException {
        AttributeModifier modifier = getColumnAttributeModifier(node);
        if (modifier != null) {
            return new AttributeModifier[] { modifier };
        } else {
            return null;
        }
    }
}

