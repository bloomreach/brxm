/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.util.KeyValue;

/**
 * GenericResponseHeadersValve
 * 
 * @version $Id$
 */
public class GenericResponseHeadersValve extends AbstractValve {
    
    private List<KeyValue<String, Object>> settableHeaders;
    private List<KeyValue<String, Object>> addableHeaders;
    
    public void setSettableHeaders(List<KeyValue<String, Object>> settableHeaders) {
        this.settableHeaders = settableHeaders;
    }
    
    public void setAddableHeaders(List<KeyValue<String, Object>> addableHeaders) {
        this.addableHeaders = addableHeaders;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletResponse servletResponse = context.getServletResponse();
        
        String key = null;
        Object value = null;
        
        if (settableHeaders != null) {
            for (KeyValue<String, Object> pair : settableHeaders) {
                key = pair.getKey();
                value = pair.getValue();
                
                if (value != null) {
                    if (value instanceof Long) {
                        servletResponse.setDateHeader(key, ((Long) value).longValue());
                    } else if (value instanceof Integer) {
                        servletResponse.setIntHeader(key, ((Integer) value).intValue());
                    } else {
                        servletResponse.setHeader(key, value.toString());
                    }
                }
            }
        }
        
        if (addableHeaders != null) {
            for (KeyValue<String, Object> pair : addableHeaders) {
                key = pair.getKey();
                value = pair.getValue();
                
                if (value != null) {
                    if (value instanceof Long) {
                        servletResponse.addDateHeader(key, ((Long) value).longValue());
                    } else if (value instanceof Integer) {
                        servletResponse.addIntHeader(key, ((Integer) value).intValue());
                    } else {
                        servletResponse.addHeader(key, value.toString());
                    }
                }
            }
        }
        
        // continue
        context.invokeNext();
    }
}
