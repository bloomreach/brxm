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
package org.hippoecm.hst.jaxrs.cxf;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class QueryStringReplacingInterceptor extends AbstractPhaseInterceptor<Message> {

    private Map<String, String> paramNameReplaces;
    private String additionalQueryString;

    public QueryStringReplacingInterceptor() {
        super(Phase.UNMARSHAL);
    }

    public void setParamNameReplaces(Map<String, String> paramNameReplaces) {
        if (paramNameReplaces == null) {
            this.paramNameReplaces = null;
        } else {
            this.paramNameReplaces = new HashMap<String, String>(paramNameReplaces);
        }
    }

    public void setAdditionalQueryString(String additionalQueryString) {
        this.additionalQueryString = additionalQueryString;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        String oldQueryString = (String) message.get(Message.QUERY_STRING);
        String newQueryString = oldQueryString;
        
        if (!StringUtils.isEmpty(newQueryString) && paramNameReplaces != null && !paramNameReplaces.isEmpty()) {
            for (Map.Entry<String, String> entry : paramNameReplaces.entrySet()) {
                String oldParamName = entry.getKey();
                String newParamName = entry.getValue();
                newQueryString = newQueryString.replaceAll("(^|&)(" + oldParamName + ")(=?)([^&;]*)", "$1" + newParamName + "$3$4");
            }
        }
        
        if (!StringUtils.isBlank(additionalQueryString)) {
            if (StringUtils.isBlank(newQueryString)) {
                newQueryString = additionalQueryString;
            } else {
                StringBuilder sb = new StringBuilder(newQueryString.length() + additionalQueryString.length() + 1);
                sb.append(newQueryString).append('&').append(additionalQueryString);
                newQueryString = sb.toString();
            }
        }
        
        if (!StringUtils.equals(oldQueryString, newQueryString)) {
            message.put(Message.QUERY_STRING, newQueryString);
        }
    }

}
