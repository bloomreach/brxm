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
package org.hippoecm.frontend.translation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTranslationProvider implements IDataProvider<HippoLocale> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentTranslationProvider.class);

    private final ILocaleProvider provider;
    private transient Map<String, HippoLocale> locales;
    private JcrNodeModel model;

    public DocumentTranslationProvider(JcrNodeModel docModel, final ILocaleProvider provider) {
        this.model = docModel;
        this.provider = provider;
    }

    private void load() {
        if (locales == null) {
            locales = new TreeMap<String, HippoLocale>();

            Node document = model.getObject();
            if (document != null) {
                try {
                    String id = document.getProperty(HippoTranslationNodeType.ID).getString();
                    Query query = document.getSession().getWorkspace().getQueryManager().createQuery(
                            "SELECT " + HippoTranslationNodeType.LOCALE + " FROM " + HippoTranslationNodeType.NT_TRANSLATED + " WHERE " + HippoTranslationNodeType.ID + "='" + id + "'",
                            Query.SQL);
                    final QueryResult result = query.execute();
                    final RowIterator rowIterator = result.getRows();
                    while (rowIterator.hasNext()) {
                        final Row row = rowIterator.nextRow();
                        final Value value = row.getValue(HippoTranslationNodeType.LOCALE);
                        HippoLocale locale = provider.getLocale(value.getString());
                        locales.put(locale.getName(), locale);
                    }
                } catch (RepositoryException ex) {
                    log.error("Error retrieving translations of document " + model.getItemModel().getPath());
                }
            }
        }
    }

    public Iterator<? extends HippoLocale> iterator(int first, int count) {
        load();

        List<HippoLocale> values = new ArrayList<HippoLocale>(locales.values());
        return values.subList(first, first + count).iterator();
    }

    public IModel<HippoLocale> model(HippoLocale object) {
        final String name = object.getName();
        return new LoadableDetachableModel<HippoLocale>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected HippoLocale load() {
                return provider.getLocale(name);
            }
        };
    }

    public boolean contains(String locale) {
        load();
        return locales.containsKey(locale);
    }

    public int size() {
        load();
        return locales.size();
    }

    public void detach() {
        locales = null;
        model.detach();
    }
}
