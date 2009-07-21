/*
 *  Copyright 2009 Hippo.
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

package org.hippoecm.hst.plugins.frontend.editor.validators;

import org.apache.wicket.validation.IValidatable;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext.HstSitemapContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.SitemapItem;

public class UniqueSitemapItemValidator extends NodeUniqueValidator<SitemapItem> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private HstSitemapContext context;

    public UniqueSitemapItemValidator(BeanProvider<SitemapItem> provider, HstSitemapContext context) {
        super(provider);
        this.context = context;
    }

    @Override
    protected String getNewNodeName(IValidatable validatable, SitemapItem bean) {
        return context.encodeMatcher((String) validatable.getValue());
    }
}
