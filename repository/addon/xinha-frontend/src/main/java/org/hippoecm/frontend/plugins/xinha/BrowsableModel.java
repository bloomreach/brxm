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
package org.hippoecm.frontend.plugins.xinha;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.ILinkDecorator;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor;

/**
 * Model that decorates the links in html.
 */
public class BrowsableModel implements IModel<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private ILinkDecorator linkDecorator;
    private IModel<String> bare;

    public BrowsableModel(IModel<String> bare, ILinkDecorator linkDecorator) {
        this.bare = bare;
        this.linkDecorator = linkDecorator;
    }

    public String getObject() {
        String text = bare.getObject();
        if (text != null) {
            return RichTextProcessor.decorateLinks(text, linkDecorator);
        }
        return null;
    }

    public void setObject(String object) {
        throw new UnsupportedOperationException();
    }

    public void detach() {
        bare.detach();
    }
    

}
