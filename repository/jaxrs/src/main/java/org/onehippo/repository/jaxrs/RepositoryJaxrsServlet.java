/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.jaxrs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used to load and initialize the {@link RepositoryJaxrsService}. This servlet is configured in the Hippo
 * CMS application's web.xml under <code>/ws/</code>.
 */
public class RepositoryJaxrsServlet extends HttpServlet {

    private static final String SHOW_SERVICE_LIST_PAGE_PARAM = "show-service-list-page";

    private RepositoryJaxrsService service;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Boolean hideServiceListPage = !Boolean.valueOf(config.getInitParameter(SHOW_SERVICE_LIST_PAGE_PARAM));

        Map<String, String> properties = new HashMap<>();
        properties.put("hide-service-list-page", hideServiceListPage.toString());
        service = RepositoryJaxrsService.init(config, properties);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service.invoke(req, resp);
    }

    @Override
    public void destroy() {
        service.destroy();
        super.destroy();
    }
}
