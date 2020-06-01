/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;

import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.DomainAuth;

import static java.util.Comparator.comparing;

public class DomainDataProvider extends SortableDataProvider<DomainAuth, String> {

    private static final Logger log = LoggerFactory.getLogger(DomainDataProvider.class);

    private static final Comparator<DomainAuth> nameComparator = comparing(DomainAuth::getName);
    private static final Comparator<DomainAuth> pathComparator = comparing(DomainAuth::getPath);

    private final transient List<DomainAuth> domains = new ArrayList<>();

    public DomainDataProvider() {
        setSort("name", SortOrder.ASCENDING);
    }

    @Override
    public Iterator<DomainAuth> iterator(long first, long count) {
        domains.sort((domain1, domain2) -> {
            final int direction = getSort().isAscending() ? 1 : -1;
            switch (getSort().getProperty()) {
                case "path":
                    return direction * pathComparator.compare(domain1, domain2);
                default:
                    return direction * nameComparator.compare(domain1, domain2);
            }
        });

        final int endIndex = (int) Math.min(first + count, domains.size());
        return domains.subList((int) first, endIndex).iterator();
    }

    @Override
    public IModel<DomainAuth> model(DomainAuth domain) {
        return new DetachableDomain(domain);
    }

    @Override
    public long size() {
        return getDomainList().size();
    }

    public List<DomainAuth> getDomainList() {
        domains.clear();
        try {
            domains.addAll(SecurityManagerHelper.getDomainsManager().getDomainAuths());
        } catch (RepositoryException e) {
            log.error("Error while trying to retrieve domains.", e);
        }
        return domains;
    }
}
