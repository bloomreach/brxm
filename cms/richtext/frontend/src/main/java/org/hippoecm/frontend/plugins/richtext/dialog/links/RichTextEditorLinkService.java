/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.richtext.dialog.links;

import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.Node;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketModel;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorInternalLink;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorLink;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.RichTextException;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLink;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextEditorLinkService implements IClusterable {

    private static final Logger log = LoggerFactory.getLogger(RichTextEditorLinkService.class);

    /**
     * The fragment ID can contain the following characters (from http://tools.ietf.org/html/rfc3986#section-3.5)
     *   0 - 9
     *   a - z
     *   A - Z
     *   ? / : @ - . _ ~ ! $ & ' ( ) * + , ; =
     *   percent-encoded characters (a % followed by two hexadecimal digits)
     */
    private static final Pattern VALID_FRAGMENT_ID = Pattern.compile("^([-?/:@._~!$&'()*+,;=a-zA-Z0-9]|%[0-9a-fA-F]{2})*$");

    private final RichTextLinkFactory factory;

    public RichTextEditorLinkService(final RichTextLinkFactory factory) {
        this.factory = factory;
    }

    public RichTextEditorInternalLink create(final Map<String, String> p) {
        final String uuid = p.get(RichTextEditorLink.UUID);
        if (uuid != null && factory.hasLink(uuid)) {
            try {
                final RichTextLink link = factory.loadLink(uuid);
                final IModel<Node> targetModel = new JcrNodeModel(link.getTargetModel().get());
                return new InternalLink(p, targetModel);
            } catch (final RichTextException e) {
                log.error("Could not load link '" + uuid + "'", e);
            }
        }
        return new InternalLink(p, null);
    }

    private class InternalLink extends RichTextEditorInternalLink {

        InternalLink(final Map<String, String> values, final IModel<Node> targetModel) {
            super(values, targetModel);
        }

        @Override
        public boolean isValid() {
            final String fragmentId = getFragmentId();
            if (StringUtils.isNotEmpty(fragmentId) && !VALID_FRAGMENT_ID.matcher(fragmentId).matches()) {
                return false;
            }

            final Model<Node> linkTarget = WicketModel.of(getLinkTarget());
            return super.isValid() && factory.isValid(linkTarget);
        }

        @Override
        public void save() {
            if (isAttacheable()) {
                try {
                    final Model<Node> linkTarget = WicketModel.of(getLinkTarget());
                    final RichTextLink link = factory.createLink(linkTarget);
                    final String uuid = link.getUuid();
                    setUuid(uuid);
                } catch (final RichTextException e) {
                    log.error("Error creating link", e);
                }
            }
        }

        @Override
        public void delete() {
            setUuid(null);
        }

    }

}
