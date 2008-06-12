/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.plugins.standardworkflow.types;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrFieldModel extends NodeModelWrapper {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeModel.class);

    public JcrFieldModel(JcrNodeModel model) {
        super(model);
    }

    public String getName() {
        try {
            if (nodeModel.getNode().hasProperty(HippoNodeType.HIPPO_NAME)) {
                return nodeModel.getNode().getProperty(HippoNodeType.HIPPO_NAME).getName();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public String getPath() {
        try {
            if (nodeModel.getNode().hasProperty(HippoNodeType.HIPPO_PATH)) {
                return nodeModel.getNode().getProperty(HippoNodeType.HIPPO_PATH).getName();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public String getTypeName() {
        try {
            return nodeModel.getNode().getProperty(HippoNodeType.HIPPO_TYPE).getString();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrFieldModel) {
            if (object != null) {
                JcrFieldModel otherModel = (JcrFieldModel) object;
                return new EqualsBuilder().append(getName(), otherModel.getName()).append(getPath(), otherModel.getPath()).isEquals();
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).append(getPath()).toHashCode();
    }
}
