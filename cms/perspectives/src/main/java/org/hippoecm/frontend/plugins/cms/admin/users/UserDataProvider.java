/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;

public class UserDataProvider extends SearchableDataProvider<User> {

    private static final long serialVersionUID = 1L;
    private static final String QUERY_USER_LIST = "SELECT * FROM hipposys:user where (hipposys:system <> 'true' or hipposys:system IS NULL)";

    private static transient List<User> userList = new ArrayList<User>();
    private static volatile boolean dirty = true;

    public UserDataProvider() {
        super(QUERY_USER_LIST);
    }

    @Override
    public IModel<User> model(final User user) {
        return new DetachableUser(user);
    }

    @Override
    protected User createBean(final Node node) throws RepositoryException {
        return new User(node);
    }

    @Override
    protected List<User> getList() {
        return userList;
    }

    @Override
    protected boolean isDirty() {
        return dirty;
    }

    @Override
    protected void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public static void setDirty() {
        dirty = true;
    }

}
