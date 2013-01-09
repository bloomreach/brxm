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
package org.hippoecm.hst.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Abstract supporting class for all tags that can contain hst:param tags
 */

public abstract class ParamContainerTag extends TagSupport {
       
    private static final long serialVersionUID = 1L;

    protected Map<String, List<String>> parametersMap = 
        new HashMap<String, List<String>>();

    protected List<String> removedParametersList = 
        new ArrayList<String>();


    protected void cleanup() {
        parametersMap.clear();
        removedParametersList.clear();
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        return EVAL_BODY_INCLUDE;
    }
    
    /**
     * Adds a key,value pair to the parameter map. 
     * @param key String
     * @param value String
     * @return void
     */
    protected void addParameter(String key,String value) {
        if((key == null) || (key.length() == 0)){
            throw new IllegalArgumentException(
                    "the argument key must not be null or empty!");
        }
        
        if((value == null) || (value.length() == 0)){//remove parameter
            if(parametersMap.containsKey(key)){
                parametersMap.remove(key);
            }
            removedParametersList.add(key);
        }
        else{//add value
            List<String> valueList = null;
        
            if(parametersMap.containsKey(key)){
                valueList = parametersMap.get(key);//get old value list                 
            }
            else{
                valueList = new ArrayList<String>();// create new value list                        
            }
        
            valueList.add(value);
        
            parametersMap.put(key, valueList);
        }
    }
    
}
