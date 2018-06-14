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
package org.hippoecm.hst.test.mock;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;

import org.springframework.mock.web.MockServletContext;

public class HstMockServletContext extends MockServletContext {
    
    protected List<PatternAndRequestDispatcher> patternAndDispatchers;
    
    public void setRequestDispatchers(List<Map<String, RequestDispatcher>> dispatchers) {
        this.patternAndDispatchers = new LinkedList<PatternAndRequestDispatcher>();
        
        for (Map<String, RequestDispatcher> item : dispatchers) {
            for (Map.Entry<String, RequestDispatcher> entry : item.entrySet()) {
                Pattern pattern = Pattern.compile(entry.getKey());
                this.patternAndDispatchers.add(new PatternAndRequestDispatcher(pattern, entry.getValue()));
            }
        }
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        RequestDispatcher dispatcher = null;
        
        for (PatternAndRequestDispatcher pard : this.patternAndDispatchers) {
            Matcher matcher = pard.pattern.matcher(path);
            
            if (matcher.matches()) {
                dispatcher = pard.dispatcher;
                
                if (dispatcher instanceof HstMockRequestDispatcher) {
                    ((HstMockRequestDispatcher) dispatcher).setUrl(path);
                }
                
                break;
            }
        }
        
        if (dispatcher == null) {
            dispatcher = super.getRequestDispatcher(path);
        }
        
        return dispatcher;
    }
    
    protected class PatternAndRequestDispatcher {
        
        protected Pattern pattern;
        protected RequestDispatcher dispatcher;
        
        protected PatternAndRequestDispatcher(Pattern pattern, RequestDispatcher dispatcher) {
            this.pattern = pattern;
            this.dispatcher = dispatcher;
        }
    }
}
