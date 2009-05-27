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
package org.hippoecm.hst.component.support.forms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.hst.core.component.HstRequest;

/**
 * A simple Form object just holding a fieldname <--> fieldvalue HashMap
 *
 */
public class FormMap {

    private final static String MESSAGE_PREFIX = "msg-key::";
    
    private Map<String,String> formMap = new HashMap<String,String>();
    private Map<String,String> messages = null;
    private String predecessorUUID = null;
    
    
    public FormMap(){
        // empty form
    }
    public FormMap(HstRequest request, List<String> fieldNames){
        // lets populate from
        this(request,fieldNames.toArray(new String[fieldNames.size()]));
    }
    
    public FormMap(HstRequest request, String[] fieldNames){
        for(String name : fieldNames) {
            String value = request.getParameter(name) == null ? "" : request.getParameter(name);
            formMap.put(name, value);
        }
    }
    
    public FormMap(Map<String, String> paramMap) {
        formMap.putAll(paramMap);
    }
    
    public void addFormField(String fieldName, String fieldValue) {
        formMap.put(fieldName, fieldValue);
    }
    
    public void addMessage(String name, String value) {
        formMap.put(getMessageKeyPrefix()+name, value);
    }
    
    public void setPrevious(String uuid) {
        this.predecessorUUID = uuid;
    }
    
    public String getField(String name) {
        return formMap.get(name);
    }
    
    public Map<String,String> getValue(){
        return formMap;
    }
    
    public Map<String,String> getMessage(){
        if(messages != null) {
            return messages;
        } else {
            messages = new HashMap<String,String>();
            for(Entry<String,String> entry : formMap.entrySet()) {
                if(entry.getKey().startsWith(getMessageKeyPrefix())) {
                    messages.put(entry.getKey().substring(getMessageKeyPrefix().length()), entry.getValue());
                }
            }
        }
        return messages;
    }
    
    public String getPrevious(){
        return this.predecessorUUID;
    }
    
    public Map<String,String> getFormMap(){
        return formMap;
    }
    
    /**
     * Override this method if you need a different prefix
     * @return the message prefix. 
     */
    public String getMessageKeyPrefix(){
        return MESSAGE_PREFIX;
    }
}

