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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;

public class CssClassAppender extends AttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public CssClassAppender(IModel<String> model) {
        super("class", true, model);
    }

    @Override
    protected String newValue(final String currentValue, final String replacementValue) {
        if(currentValue == null) {
            if(replacementValue == null)
               return "";
            return replacementValue;
        } else if(replacementValue == null)
            return currentValue;
        return currentValue + " " + replacementValue;
    }
}
