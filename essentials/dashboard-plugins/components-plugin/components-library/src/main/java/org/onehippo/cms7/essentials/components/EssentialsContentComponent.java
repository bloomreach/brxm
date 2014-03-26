/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

/**
 * Hippo HST component for simple (document detail) request handling, it just fetches sitemap mapped bean.
 * Use {@code EssentialsDocumentComponent} for picked documents (e.g. banner and alike)
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.components.EssentialsDocumentComponent
 */

public class EssentialsContentComponent extends CommonComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        setContentBeanWith404(request, response);
    }
}
