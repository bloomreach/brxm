/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.rest.ctx;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;

/**
 * @version "$Id: RestContext.java 174715 2013-08-22 13:48:50Z mmilicevic $"
 */
public interface RestContext {

    boolean isMinimalDataSet();

    void setMinimalDataSet(boolean minimalDataSet);

    HttpServletRequest getRequest();

    HstRequestContext getRequestContext();

    int getResultLimit();

    void setResultLimit(int resultLimit);

    String getScope();

    boolean isAbsolutePath();

    void setAbsolutePath(boolean absolutePath);

    void setScope(String path);

    BaseRestResource getResource();

    HippoFolderBean getGalleryFolder();

    Map<String, String> getContextParams();
}
