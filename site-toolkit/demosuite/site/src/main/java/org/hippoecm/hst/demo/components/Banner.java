package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.pagecomposer.annotations.ParameterInfo;

@ParameterInfo(className = BannerInfo.class)
public class Banner extends BaseHstComponent {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        String path = getComponentConfiguration().getCanonicalPath();
        int index = path.lastIndexOf('/');
        String parentPath = path.substring(0, index);
        String parentName = parentPath.substring(parentPath.lastIndexOf('/') + 1);
        request.setAttribute("myName", parentName + ":" + path.substring(index + 1));
    }
}
