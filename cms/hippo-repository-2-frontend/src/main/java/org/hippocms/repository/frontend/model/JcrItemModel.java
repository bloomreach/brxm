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
package org.hippocms.repository.frontend.model;

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippocms.repository.frontend.UserSession;

public class JcrItemModel extends LoadableDetachableModel {
    private static final long serialVersionUID = 1L;

    private String path;

    // constructor

    public JcrItemModel(Item item) {
        super(item);
        try {
            this.path = item.getPath();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // LoadableDetachableModel

    protected Object load() {
        Item result = null;
        try {
            UserSession sessionProvider = (UserSession)Session.get();
            result = sessionProvider.getJcrSession().getItem(path);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

}
