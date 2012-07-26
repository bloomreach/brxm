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
package org.hippoecm.frontend.translation.list.resolvers;

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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard attributes of a hippotranslation:translated document.  Figures out what css classes should be used to
 * represent the state.  Can be used with handles, documents and (document) versions.
 */
public class TranslationRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TranslationRenderer.class);

    private ILocaleProvider provider;

    public TranslationRenderer(ILocaleProvider provider) {
        this.provider = provider;
    }

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        Node document = null;
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            node = node.getNode(node.getName());
            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                document = node;
            }
        } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                document = node;
            }
        }
        if (document != null) {
            return new TranslationList(id, document);
        }
        return new EmptyPanel(id);
    }

    private final class DocumentTranslationProvider implements IDataProvider<HippoLocale> {
        private static final long serialVersionUID = 1L;

        private transient Map<String, HippoLocale> locales;
        private JcrNodeModel model;

        private DocumentTranslationProvider(JcrNodeModel docModel) {
            this.model = docModel;
            model = docModel;
        }

        private void load() {
            if (locales == null) {
                locales = new TreeMap<String, HippoLocale>();

                Node document = model.getObject();
                if (document != null) {
                    try {
                        String id = document.getProperty(HippoTranslationNodeType.ID).getString();
                        Query query = document.getSession().getWorkspace().getQueryManager().createQuery(
                                "SELECT " + HippoTranslationNodeType.LOCALE
                              + " FROM " + HippoTranslationNodeType.NT_TRANSLATED
                              + " WHERE " + HippoTranslationNodeType.ID + "='" + id + "'",
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

        public int size() {
            load();
            return locales.size();
        }

        public void detach() {
            locales = null;
            model.detach();
        }
    }

    private class TranslationList extends Panel {

        private static final long serialVersionUID = 1L;
        private String locale;

        public TranslationList(String id, Node document) throws RepositoryException {
            super(id);

            add(CSSPackageResource.getHeaderContribution(getClass(), "style.css"));

            locale = document.getProperty(HippoTranslationNodeType.LOCALE).getString();

            final JcrNodeModel docModel = new JcrNodeModel(document);
            add(new DataView<HippoLocale>("flags", new DocumentTranslationProvider(docModel)) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<HippoLocale> item) {
                    HippoLocale itemLocale = item.getModelObject();
                    if (itemLocale.getName().equals(locale)) {
                        Image img = new Image("img", itemLocale.getIcon(IconSize.TINY, LocaleState.EXISTS));
                        img.add(new AttributeAppender("class", new Model<String>("hippo-translation-current"), " "));
                        item.add(img);
                    } else {
                        item.add(new Image("img", itemLocale.getIcon(IconSize.TINY, LocaleState.EXISTS)));
                    }
                }

            });
        }
    }
}
