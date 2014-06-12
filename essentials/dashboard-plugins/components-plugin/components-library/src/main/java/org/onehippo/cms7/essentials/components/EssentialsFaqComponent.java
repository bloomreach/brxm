/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsFaqComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */

@ParametersInfo(type = EssentialsFaqComponentInfo.class)
public class EssentialsFaqComponent extends EssentialsListComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsFaqComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {

        final EssentialsFaqComponentInfo paramInfo = getComponentParametersInfo(request);
        // check if single document is used:
        if(!Strings.isNullOrEmpty(paramInfo.getFaqDocument())){
            setContentBeanWith404(request, response);
            return;
        }

        final String path = paramInfo.getPath();
        if(paramInfo.getFolderOrder() !=null && paramInfo.getFolderOrder() && !Strings.isNullOrEmpty(path)){

            final HippoFolderBean bean = getHippoBeanForPath(path, HippoFolder.class);
            if(bean==null){
                return;
            }
            final List<HippoDocumentBean> documents = bean.getDocuments();
            //..

        }

        super.doBeforeRender(request, response);

    }
}
