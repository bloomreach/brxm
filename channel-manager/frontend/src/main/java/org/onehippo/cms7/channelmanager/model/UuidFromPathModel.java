/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model that converts JCR UUIDs to JCR paths and stores the paths as strings in a delegate model.
 */
public class UuidFromPathModel implements IModel<String> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UuidFromPathModel.class);

    private IModel<String> delegate;

    public UuidFromPathModel(IModel<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getObject() {
        final String path = delegate.getObject();

        if (StringUtils.isNotEmpty(path)) {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                Node node = session.getNode(path);
                return node.getIdentifier();
            } catch (RepositoryException e) {
                log.warn("Cannot retrieve UUID from '" + path + "'", e);
            }
        }

        return null;
    }

    @Override
    public void setObject(final String uuid) {
        if (uuid == null) {
            delegate.setObject(null);
        } else {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

            try {
                Node node = session.getNodeByIdentifier(uuid);
                delegate.setObject(node.getPath());
            } catch (RepositoryException e) {
                log.warn("Cannot retrieve node with UUID '" + uuid + "'", e);
            }
        }
    }

    @Override
    public void detach() {
        delegate.detach();
    }

}
