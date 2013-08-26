/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.ckeditor.dialog.links;

import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.ckeditor.dialog.model.CKEditorLink;
import org.hippoecm.frontend.plugins.ckeditor.dialog.model.InternalCKEditorLink;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CKEditorLinkService implements IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CKEditorLinkService.class);

    private String editorId;
    private IRichTextLinkFactory factory;

    public CKEditorLinkService(IRichTextLinkFactory factory, String editorId) {
        this.factory = factory;
        this.editorId = editorId;
    }

    public InternalCKEditorLink create(Map<String, String> p) {
        final String path = p.get(CKEditorLink.HREF);
        if (path != null) {
            final String decodedPath = RichTextUtil.decode(path);
            if (factory.getLinks().contains(decodedPath)) {
                try {
                    final RichTextLink link = factory.loadLink(decodedPath);
                    return new InternalLink(p, link.getTargetId());
                } catch (RichTextException e) {
                    log.error("Could not load link '" + path + "'", e);
                }
            }
        }
        return new InternalLink(p, null);
    }

    public void detach() {
        factory.detach();
    }

    private class InternalLink extends InternalCKEditorLink {
        private static final long serialVersionUID = 1L;

        public InternalLink(Map<String, String> values, IDetachable targetId) {
            super(values, targetId);
        }

        @Override
        public boolean isValid() {
            return super.isValid() && factory.isValid(getLinkTarget());
        }

        public void save() {
            if (isAttacheable()) {
                try {
                    final RichTextLink link = factory.createLink(getLinkTarget());
                    setHref(RichTextUtil.encode(link.getName()));
                } catch (RichTextException e) {
                    log.error("Error creating link", e);
                }
            }
        }

        public void delete() {
            setHref(null);
        }

    }

}
