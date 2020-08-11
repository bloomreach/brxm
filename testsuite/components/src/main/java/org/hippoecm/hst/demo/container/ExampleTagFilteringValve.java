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
package org.hippoecm.hst.demo.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.Valve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;

/**
 * Sample tag filtering valve. Alternates between the tags 'aap' or 'noot' and none.
 */
public class ExampleTagFilteringValve implements Valve {

    static int counter = 0;

    @Override
    public void initialize() throws ContainerException {
    }
    
    @Override
    public void destroy() {
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();

        Cookie abcookie = null;
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("TagFiltering".equals(cookie.getName())) {
                    abcookie = cookie;
                    break;
                }
            }
        }
 
        List<String> options = new ArrayList<String>(2);
        options.add("aap");
        options.add("noot");

        String option = null;
        String value = null;
        if (abcookie != null) {
            value = abcookie.getValue();
            if ("<null>".equals(value)) {
                option = null;
                value = null;
            } else if (options.contains(value)) {
                option = value;
            } else {
                value = null;
            }
        }
        if (value == null) {
            int index = counter++ % (options.size() + 1);
            if (index > 0) {
                option = options.get(index - 1);
                value = option;
            } else {
                option = null;
                value = "<null>";
            }

            if (abcookie == null) {
                abcookie = new Cookie("TagFiltering", value);
                servletResponse.addCookie(abcookie);
            } else {
                abcookie.setValue(value);
            }
        }

        if (option != null) {
            Set<String> conditions = new TreeSet<String>();
            conditions.add(option);
            requestContext.setComponentFilterTags(conditions);
        }

        context.invokeNext();
    }

}
