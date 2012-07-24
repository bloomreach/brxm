package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = BlockInfo.class)
public class Block extends BaseHstComponent {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id:$";

    public static final Logger log = LoggerFactory.getLogger(Block.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        BlockInfo paramsInfo = getParametersInfo(request);

        request.setAttribute("info",  paramsInfo);
    }
}
