package org.onehippo.forge.contentblocksdemo.components;

import org.onehippo.forge.contentblocksdemo.componentsinfo.PageableListInfo;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = PageableListInfo.class)
public class Overview extends BaseComponent {

    public static final Logger log = LoggerFactory.getLogger(Overview.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {

       PageableListInfo info = getComponentParametersInfo(request);
       HippoBean scope = request.getRequestContext().getContentBean();

       if (scope == null) {
           response.setStatus(404);
           log.info("For an Overview component there must be a content bean available to search below. Cannot create an overview");
           return;
       }
       createAndExecuteSearch(request, info, scope, null);
    }

}
