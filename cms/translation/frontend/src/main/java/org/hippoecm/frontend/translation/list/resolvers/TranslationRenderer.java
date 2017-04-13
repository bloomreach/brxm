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
package org.hippoecm.frontend.translation.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.translation.DocumentTranslationProvider;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.frontend.translation.TranslationUtil;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;

/**
 * Standard attributes of a hippotranslation:translated document.  Figures out what css classes should be used to
 * represent the state.  Can be used with handles, documents and (document) versions.
 */
public class TranslationRenderer extends AbstractNodeRenderer {

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

        if ((document != null) &&
            (!TranslationUtil.isNtTranslated(document.getParent().getParent()) &&
                (!TranslationUtil.isNtTranslated(document) ||
                    !provider.isKnown(document.getProperty(HippoTranslationNodeType.LOCALE).getString())))) {
            return new TranslationList(id, document);
        }
        return new EmptyPanel(id);
    }

    private class TranslationList extends Panel {

        private String locale;

        public TranslationList(String id, Node document) throws RepositoryException {
            super(id);

            locale = document.getProperty(HippoTranslationNodeType.LOCALE).getString();

            final JcrNodeModel docModel = new JcrNodeModel(document);
            add(new DataView<HippoLocale>("flags", new DocumentTranslationProvider(docModel, provider)) {
                @Override
                protected void populateItem(Item<HippoLocale> item) {
                    HippoLocale itemLocale = item.getModelObject();
                    Image img = new CachingImage("img", itemLocale.getIcon(IconSize.M, LocaleState.EXISTS));
                    if (itemLocale.getName().equals(locale)) {
                        img.add(new AttributeAppender("class", Model.of("hippo-translation-current"), " "));
                    }
                    item.add(img);
                }
            });
        }
    }
}
