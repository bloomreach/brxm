/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.behavioral;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.behavioral.BehavioralData;
import org.hippoecm.hst.behavioral.BehavioralDataStore;


public class BehavioralDataHttpSessionStore implements BehavioralDataStore {

    public static final String SESSION_ATTR_NAME = BehavioralDataHttpSessionStore.class.getName()+".session";

    @Override
    public List<BehavioralData> readBehavioralData(HttpServletRequest request) {
        
        HttpSession session = request.getSession(true);
       
        List<BehavioralData> behavioralDataList = (List<BehavioralData>) session.getAttribute(SESSION_ATTR_NAME);
        if (behavioralDataList == null) {
            behavioralDataList = new ArrayList<BehavioralData>();
        }
        return behavioralDataList;
    }
   
    @Override
    public void storeBehavioralData(HttpServletRequest request, HttpServletResponse response, List<BehavioralData> behavioralDataList) {
        
        HttpSession session = request.getSession(true);
        
        session.setAttribute(SESSION_ATTR_NAME, behavioralDataList);
        
    }
}
