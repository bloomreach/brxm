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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;

public class GroupDataProvider extends SearchableDataProvider<Group> {

    private static final long serialVersionUID = 1L;
    private static final String QUERY_GROUP_LIST = "SELECT * FROM hipposys:group where (hipposys:system <> 'true' or hipposys:system IS NULL)";

    private static transient List<Group> groupList = new ArrayList<Group>();
    private static volatile boolean dirty = true;

    public GroupDataProvider() {
        super(QUERY_GROUP_LIST);
    }

    @Override
    protected Group createBean(final Node node) throws RepositoryException {
        return new Group(node);
    }

    @Override
    public IModel<Group> model(final Group group) {
        return new DetachableGroup(group);
    }

    @Override
    protected List<Group> getList() {
        return groupList;
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
