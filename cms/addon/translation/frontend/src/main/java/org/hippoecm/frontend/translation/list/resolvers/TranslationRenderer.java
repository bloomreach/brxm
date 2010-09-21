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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.PackagedResourceReference;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
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
 * Standard attributes of a hippotranslation:translated document.  Figures out what css classes
 * should be used to represent the state.  Can be used with handles, documents and (document)
 * versions.
 */
public class TranslationRenderer extends AbstractNodeRenderer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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

    private class TranslationList extends Panel {
        private static final long serialVersionUID = 1L;
        private String locale;
        private Set<String> localeNames;

        public TranslationList(String id, Node document) throws RepositoryException {
            super(id);

            add(CSSPackageResource.getHeaderContribution(getClass(), "style.css"));

            locale = null;
            Node localeHolder = document;
            while (localeHolder.getDepth() > 0) {
                if (localeHolder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)
                        && localeHolder.hasProperty(HippoTranslationNodeType.LOCALE)) {
                    locale = localeHolder.getProperty(HippoTranslationNodeType.LOCALE).getString();
                    break;
                }
                localeHolder = localeHolder.getParent();
            }

            localeNames = new TreeSet<String>();
            NodeIterator translationNodes = document.getNode(HippoTranslationNodeType.TRANSLATIONS).getNodes();
            while (translationNodes.hasNext()) {
                Node translation = translationNodes.nextNode();
                localeNames.add(translation.getName());
            }

            add(new DataView<HippoLocale>("flags", new ListDataProvider<HippoLocale>((List<HippoLocale>) provider
                    .getLocales())) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<HippoLocale> item) {
                    HippoLocale itemLocale = item.getModelObject();
                    if (itemLocale.getName().equals(locale)) {
                        Image img = new Image("img", itemLocale.getIcon(IconSize.TINY, LocaleState.EXISTS));
                        img.add(new AttributeAppender("class", new Model<String>("hippo-translation-current"), " "));
                        item.add(img);
                    } else if (localeNames.contains(itemLocale.getName())) {
                        item.add(new Image("img", itemLocale.getIcon(IconSize.TINY, LocaleState.EXISTS)));
                    } else {
                        item.add(new Image("img", itemLocale.getIcon(IconSize.TINY, LocaleState.DISABLED)));
                    }
                }

            });
        }

    }
}
