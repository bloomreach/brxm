package org.onehippo.cms7.essentials.site.components.standard;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.essentials.site.components.BaseComponent;

public class HeaderComponent extends BaseComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        request.setAttribute("query", getAnyParameter(request, "query"));
        request.setAttribute("menu", request.getRequestContext().getHstSiteMenus().getSiteMenu("main"));
    }

}
