/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippogallery:exampleAssetSet")
public class HippoAsset extends HippoDocument implements HippoAssetBean{

    private static Logger log = LoggerFactory.getLogger(HippoAsset.class);
    
    private BeanWrapper<HippoResourceBean> resource; 
    
    public HippoResourceBean getAsset(){
        if(resource != null) {
            return resource.getBean();
        }
        HippoBean bean = this.getBean("hippogallery:asset");
        if(bean instanceof HippoResourceBean) {
            resource = new BeanWrapper<HippoResourceBean>((HippoResourceBean)bean);
        }else if(bean == null) {
           log.debug("hippogallery:asset not found for node '{}'", this.getPath());
           resource =  new BeanWrapper<HippoResourceBean>((HippoResourceBean)null);
        } else {
            log.warn("Expected resource of type HippoResourceBean but found '{}'. Return null", bean.getClass().getName() );
            resource =  new BeanWrapper<HippoResourceBean>((HippoResourceBean)null);
        }
        return resource.getBean();
    }
}
