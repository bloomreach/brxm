/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.comparators.PropertyComparator;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

import static java.util.Comparator.comparing;

public class GroupDataProvider extends SearchableDataProvider<Group> {

    private static final long serialVersionUID = 1L;

    private static final String QUERY_ALL_GROUP_LIST = "SELECT * " +
                " FROM " + HippoNodeType.NT_GROUP +
                " WHERE (hipposys:system <> 'true' OR hipposys:system IS NULL)";

    private static final Collator collator = Collator.getInstance(UserSession.get().getLocale());
    private static final Comparator<String> propertyComparator = new PropertyComparator(collator);
    private static final Comparator<Group> groupnameComparator = comparing(Group::getGroupname, propertyComparator);

    public GroupDataProvider() {
        super(QUERY_ALL_GROUP_LIST, "/hippo:configuration/hippo:groups", HippoNodeType.NT_GROUP, HippoNodeType.NT_GROUPFOLDER);
        setSort("groupname", SortOrder.ASCENDING);
        setIncludePrimaryTypes(new String[] {HippoNodeType.NT_GROUP});
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
    public Iterator<Group> iterator(long first, long count) {
        final List<Group> groupList = new ArrayList<>(getList());

        groupList.sort((group1, group2) -> {
            final int direction = getSort().isAscending() ? 1 : -1;
            return direction * groupnameComparator.compare(group1, group2);
        });

        return groupList.subList((int) first, (int) Math.min(first + count, groupList.size())).iterator();
    }
}
