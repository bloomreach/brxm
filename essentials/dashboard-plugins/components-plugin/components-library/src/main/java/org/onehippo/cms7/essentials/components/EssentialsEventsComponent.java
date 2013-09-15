/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsEventsComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST component used for listing of Event document types
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsEventsComponentInfo.class)
public class EssentialsEventsComponent extends EssentialsListComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsEventsComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        // TODO implement
    }
}
