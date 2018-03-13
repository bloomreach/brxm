/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ParametersInfo(type = BannerInfo.class)
public class Banner extends BaseHstComponent {
    public static final Logger log = LoggerFactory.getLogger(Banner.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        BannerInfo paramsInfo = getComponentParametersInfo(request);

        int width = paramsInfo.getBannerWidth();

        request.setAttribute("borderColor", paramsInfo.getBorderColor());

        request.setModel("document", request.getRequestContext().getContentBean());
        request.setModel("content", paramsInfo.getContent());

        String path = getComponentConfiguration().getCanonicalPath();
        int index = path.lastIndexOf('/');
        String parentPath = path.substring(0, index);
        String parentName = parentPath.substring(parentPath.lastIndexOf('/') + 1);
        request.setAttribute("myName", parentName + ":" + path.substring(index + 1));

        String bannerPath = paramsInfo.getPath();
        request.setAttribute("bannerPath", bannerPath);
    }
}
