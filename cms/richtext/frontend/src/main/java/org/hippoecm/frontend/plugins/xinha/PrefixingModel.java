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
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixingModel implements IModel<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PrefixingModel.class);

    private IImageURLProvider decorator;
    private IModel<String> bare;

    public PrefixingModel(IModel<String> bare, IImageURLProvider decorator) {
        this.bare = bare;
        this.decorator = decorator;
    }

    public String getObject() {
        String text = bare.getObject();
        if (text != null) {
            return RichTextProcessor.prefixImageLinks(text, decorator);
        }
        return null;
    }

    public void setObject(String object) {
        if (object != null) {
            bare.setObject(RichTextProcessor.restoreFacets(object));
        } else {
            bare.setObject(null);
        }
    }

    public void detach() {
        bare.detach();
    }

}
