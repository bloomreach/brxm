/*
 * Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.servlet;

import javax.servlet.ServletConfig;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

/**
 * Tests {@link BinariesServlet}. Since the tested class is originally written by Hippo,
 * only the added logic is tested. The original logic is left untested.
 */
public class AbstractTestBinariesServlet {

    protected ServletConfig getNoParamServletConfigMock() {
        ServletConfig servletConfig = createMock(ServletConfig.class);
        expect(servletConfig.getInitParameter("contentDispositionContentTypes")).andReturn(null);
        expect(servletConfig.getInitParameter("contentDispositionFilenameProperty")).andReturn(null);
        expect(servletConfig.getInitParameter("baseBinariesContentPath")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryResourceNodeType")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryDataPropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryMimeTypePropName")).andReturn(null);
        expect(servletConfig.getInitParameter("binaryLastModifiedPropName")).andReturn(null);
        expect(servletConfig.getInitParameter("contentDispositionFilenameEncoding")).andReturn(null);

        expect(servletConfig.getInitParameter("set-expires-headers")).andReturn(null);
        expect(servletConfig.getInitParameter("set-content-length-header")).andReturn(null);
        expect(servletConfig.getInitParameter("forceContentDispositionRequestParamName")).andReturn(null);
        expect(servletConfig.getInitParameter("use-accept-ranges-header")).andReturn("false");
        return servletConfig;
    }

}
