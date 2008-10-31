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
package org.hippoecm.frontend.i18n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSearchingProvider implements IModelProvider<IModel> {
    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(JcrSearchingProvider.class);

    public IModel getModel(Map<String, String> criteria) {
        try {
            QueryManager qMgr = ((UserSession) Session.get()).getQueryManager();
            String strQuery = "//element(" + ISO9075Helper.encodeLocalName(criteria.get("hippo:key"))
                    + ", hippo:localization)[@hippo:language='" + criteria.get("hippo:language") + "']";
            System.out.println("Query: " + strQuery);
            Query query = qMgr.createQuery(strQuery, Query.XPATH);
            NodeIterator nodes = query.execute().getNodes();
            if (nodes.getSize() > 0) {
                List<LocalizationNodeWrapper> list = new ArrayList<LocalizationNodeWrapper>((int) nodes.getSize());
                while (nodes.hasNext()) {
                    list.add(new LocalizationNodeWrapper(nodes.nextNode(), criteria));
                }
                LocalizationNodeWrapper best = Collections.max(list);
                return best.getModel();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}