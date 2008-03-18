/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.model;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrItemModel extends LoadableDetachableModel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrItemModel.class);

    protected String path;

    // constructors

    public JcrItemModel(Item item) {
        super(item);
        try {
            this.path = item.getPath();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    public JcrItemModel(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public boolean exists() {
        boolean result = false;
        try {
            UserSession sessionProvider = (UserSession) Session.get();
            result = sessionProvider.getJcrSession().itemExists(path);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    public JcrItemModel getParentModel() {
        int idx = path.lastIndexOf('/');
        if (idx > 0) {
            String parent = path.substring(0, path.lastIndexOf('/'));
            return new JcrItemModel(parent);
        } else if (idx == 0) {
            if (path.equals("/")) {
                return null;
            }
            return new JcrItemModel("/");
        } else {
            log.error("Unrecognised path " + path);
            return null;
        }
    }

    // LoadableDetachableModel

    @Override
    protected Object load() {
        Item result = null;
        try {
            UserSession sessionProvider = (UserSession) Session.get();
            result = sessionProvider.getJcrSession().getItem(path);
        } catch (RepositoryException e) {
            log.info(e.getMessage());
        }
        return result;
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", path).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrItemModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrItemModel itemModel = (JcrItemModel) object;
        return new EqualsBuilder().append(normalizePath(path), normalizePath(itemModel.path)).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(177, 3).append(path).toHashCode();
    }

    private static String normalizePath(String path) {
        if (path.length() > 0) {
            if (path.charAt(path.length() - 1) == ']') {
                return path;
            }
            return path + "[1]";
        }
        return path;
    }
}
