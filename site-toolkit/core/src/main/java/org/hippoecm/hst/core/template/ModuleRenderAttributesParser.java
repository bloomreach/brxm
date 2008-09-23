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
package org.hippoecm.hst.core.template;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.HSTHttpAttributes;

public class ModuleRenderAttributesParser {
      private HttpServletRequest request;
      private HttpSession session;
      private List attributes;
      
      public ModuleRenderAttributesParser(HttpServletRequest request) {
    	  this.request = request;
    	  this.session = request.getSession();
    	  attributes = (List) session.getAttribute(HSTHttpAttributes.MODULE_RENDER_MAP_SESSION_ATTRIBUTE);
    	  if (attributes == null) {
    		  attributes = new ArrayList();
    	  }
    	  
    	  
      }
      
      public List getModuleRenderAttributes() {
    	  return attributes;
      }
      
      private void parseAttributes() {
    	  
      }
      
      
      
       
      
      
}
