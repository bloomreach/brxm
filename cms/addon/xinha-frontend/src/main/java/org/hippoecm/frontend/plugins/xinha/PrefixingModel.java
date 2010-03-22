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
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor.ILinkDecorator;

public class PrefixingModel extends LoadableDetachableModel<String> {
    private static final long serialVersionUID = 1L;

    private String prefix;
    private IModel<String> bare;
    private ILinkDecorator linkDecorator;

    public PrefixingModel(IModel<String> bare, ILinkDecorator linkDecorator, String prefix) {
        this.bare = bare;
        this.linkDecorator = linkDecorator;
        this.prefix = prefix;
    }
    
    @Override
    protected String load() {
        String text = bare.getObject();
        if (text != null) {
            String processed = RichTextProcessor.prefixImageLinks(text, prefix);
            return RichTextProcessor.decorateLinks(processed, linkDecorator);
        } else {
            return null;
        }
    }

    @Override
    public void detach() {
        bare.detach();
        super.detach();
    }

}
