/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.standards.search;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchDataProvider implements IDataProvider<TextSearchMatch> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextSearchDataProvider.class);

    static final int DEFAULT_LIMIT = 100;

    private IModel<BrowserSearchResult> resultModel;

    public TextSearchDataProvider(IModel<BrowserSearchResult> bsrModel) {
        this.resultModel = bsrModel;
    }

    public Iterator<? extends TextSearchMatch> iterator(int first, int count) {
        BrowserSearchResult bsr = resultModel.getObject();
        QueryResult result = bsr.getQueryResult();
        if (result == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        try {
            javax.jcr.Session session = UserSession.get().getJcrSession();

            List<TextSearchMatch> resultList = new LinkedList<TextSearchMatch>();
            boolean hasExcerpt = false;
            for (String colName : result.getColumnNames()) {
                if ("rep:excerpt()".equals(colName)) {
                    hasExcerpt = true;
                    break;
                }
            }
            RowIterator rows = result.getRows();
            rows.skip(first);
            while (rows.hasNext() && (count == -1 || count-- > 0)) {
                Row row = rows.nextRow();
                try {
                    String path = row.getValue("jcr:path").getString();
                    Node node = (Node) session.getItem(path);
                    if (hasExcerpt) {
                        String excerpt = row.getValue("rep:excerpt()").getString();
                        resultList.add(new TextSearchMatch(node, excerpt));
                    } else {
                        resultList.add(new TextSearchMatch(node));
                    }
                } catch (ItemNotFoundException infe) {
                    log.warn("Item not found", infe);
                } catch (ValueFormatException vfe) {
                    log.error("Value is invalid", vfe);
                }
                return resultList.iterator();
            }
        } catch (RepositoryException e) {
            log.error("Error parsing query results[" + bsr.getQueryName() + "]", e);
        }
        return Collections.EMPTY_LIST.iterator();
    }

    public IModel<TextSearchMatch> model(TextSearchMatch object) {
        // TODO Auto-generated method stub
        return null;
    }

    public int size() {
        QueryResult result = resultModel.getObject().getQueryResult();
        if (result != null) {
            try {
                return (int) result.getRows().getSize();
            } catch (RepositoryException e) {
                log.error("Error parsing query results", e);
            }
        }
        return 0;
    }

    public void detach() {
        resultModel.detach();
    }

}
