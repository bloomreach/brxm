/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;

/**
 * Creates a list of links to security domains
 */
public class DomainLinkListPanel extends Panel {

    public DomainLinkListPanel(final String id, final Map<Domain, List<String>> domainsWithRoles,
                               final AdminBreadCrumbPanel panelToReplace) {
        super(id);

        List<Domain> domainList = new ArrayList<Domain>(domainsWithRoles.keySet());
        Collections.sort(domainList);
        ListView<Domain> domainListView = new ListView<Domain>("item", domainList) {
            @Override
            protected void populateItem(final ListItem<Domain> detachableDomainListItem) {

                List<String> roles = domainsWithRoles.get(detachableDomainListItem.getModelObject());
                Domain domain = detachableDomainListItem.getModelObject();
                String domainDisplayText = domain.getName() + " (" + createDelimitedString(roles) + ")";
                ViewDomainActionLink action = new ViewDomainActionLink(
                        "link", panelToReplace,
                        detachableDomainListItem.getModel(),
                        new Model<String>(domainDisplayText)
                );

                detachableDomainListItem.setRenderBodyOnly(true);
                detachableDomainListItem.add(action);
            }
        };

        add(domainListView);
    }

    /**
     * @param listToConvert The list which much be converted in to a comma separated string.
     * @return The string.
     */
    private static String createDelimitedString(final Collection<String> listToConvert) {
        StringBuilder sb = new StringBuilder();
        for (String listItem : listToConvert) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(listItem);
        }
        return sb.toString();
    }
}
