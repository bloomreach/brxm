/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsDocumentComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hippo HST component for simple (document detail) request handling.
 * If document path is not defined, EssentialsDocumentComponent will try to fetch sitemap mapped bean.
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsDocumentComponentInfo.class)
public class EssentialsDocumentComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsDocumentComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsDocumentComponentInfo paramInfo = getComponentParametersInfo(request);
        final String documentPath = paramInfo.getDocument();
        log.debug("Calling EssentialsDocumentComponent for document path:  [{}]", documentPath);
        setContentBean(documentPath, request, response);
    }


}
