/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeansWrapper<B extends HippoBean> {

    private static Logger log = LoggerFactory.getLogger(BeansWrapper.class);
    
    private HippoBean bean;
    private Map<String, BeanWrapper<B>> wrappedBeans = new HashMap<String, BeanWrapper<B>>();
    
    public BeansWrapper(HippoBean bean) {
       this.bean = bean;
    }
    
    
    @SuppressWarnings("unchecked")
    public B getBean(String relPath, Class<B> beanMappingClass){
        BeanWrapper<B> wrapped = wrappedBeans.get(relPath);
        if(wrapped != null) {
            return wrapped.getBean();
        } else {
            Object o = bean.getBean(relPath);
            if(o == null) {
                if(log.isDebugEnabled()) {
                    log.debug("No bean found for relPath '{}' at '{}'", relPath, bean.getPath());
                }
                wrapped = new BeanWrapper<B>(null);
                wrappedBeans.put(relPath, wrapped);
                return null;
            } else if(beanMappingClass.isAssignableFrom(o.getClass())) {
                wrapped = new BeanWrapper<B>((B)o);
                wrappedBeans.put(relPath, wrapped);
                return wrapped.getBean();
            } else {
                log.info("Cannot get '"+beanMappingClass.getName()+"' bean for relPath '{}' at '{}' because returned bean is of type '"+o.getClass().getName()+"'", relPath, bean.getPath());
                // even when null, put it in the map to avoid being refetched
                wrapped = new BeanWrapper<B>(null);
                wrappedBeans.put(relPath, wrapped);
                return null;
            }
        }
    }

    public void detach() {
        for(BeanWrapper<B> wrapper : wrappedBeans.values()) {
            if(wrapper.getBean() != null && (wrapper.getBean() instanceof HippoItem)) {
                ((HippoItem)wrapper.getBean()).detach();
            }
        }
    }
}
