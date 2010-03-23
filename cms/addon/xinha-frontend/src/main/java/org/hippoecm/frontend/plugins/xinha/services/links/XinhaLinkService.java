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

package org.hippoecm.frontend.plugins.xinha.services.links;

import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaLinkService implements IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaLinkService.class);

    private String editorId;
    private IRichTextLinkFactory factory;
    
    public XinhaLinkService(IRichTextLinkFactory factory, String editorId) {
        this.factory = factory;
        this.editorId = editorId;
    }

    public InternalXinhaLink create(Map<String, String> p) {
        String relPath = p.get(XinhaLink.HREF);
        RichTextLink rtl = factory.loadLink(RichTextUtil.decode(relPath));
        return new InternalLink(p, rtl != null ? rtl.getTargetId() : null);
    }

    public String attach(JcrNodeModel model) {
        RichTextLink rtl = factory.createLink(model);
        if (rtl != null) {
            String href = RichTextUtil.encode(rtl.getName());
            String script = "xinha_editors." + editorId + ".plugins.CreateLink.instance.createLink({"
                    + XinhaLink.HREF + ": '" + href + "', " + XinhaLink.TARGET + ": ''}, false);";
            return script;
        }
        return null;
    }

    public void detach() {
        factory.detach();
    }

    private class InternalLink extends InternalXinhaLink {
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
                if (isReplacing()) {
                    Map<String, String> values = getInitialValues();
                    String relPath = RichTextUtil.decode(values.get(XinhaLink.HREF));
                    RichTextLink rtl = factory.loadLink(relPath);
                    factory.delete(rtl);
                }
                RichTextLink rtl = factory.createLink(getLinkTarget());
                if (rtl != null) {
                    setHref(RichTextUtil.encode(rtl.getName()));
                }
            }
        }

        public void delete() {
            Map<String, String> values = getInitialValues();
            String relPath = RichTextUtil.decode(values.get(XinhaLink.HREF));
            RichTextLink rtl = factory.loadLink(relPath);
            factory.delete(rtl);
            setHref(null);
        }

    }

}
