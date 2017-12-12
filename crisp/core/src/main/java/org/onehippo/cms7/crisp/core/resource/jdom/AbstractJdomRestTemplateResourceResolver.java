/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource.jdom;

import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.AbstractRestTemplateResourceResolver;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractJdomRestTemplateResourceResolver extends AbstractRestTemplateResourceResolver {

    private Unmarshaller unmarshaller;
    private String jaxbContextPath;
    private Class<?>[] jaxbClassesToBeBound;
    private String[] jaxbPackagesToScan;
    private ResourceBeanMapper resourceBeanMapper;

    public String getJaxbContextPath() {
        return jaxbContextPath;
    }

    public void setJaxbContextPath(String jaxbContextPath) {
        this.jaxbContextPath = jaxbContextPath;
    }

    public Class<?>[] getJaxbClassesToBeBound() {
        return jaxbClassesToBeBound;
    }

    public void setJaxbClassesToBeBound(Class<?>[] jaxbClassesToBeBound) {
        this.jaxbClassesToBeBound = jaxbClassesToBeBound;
    }

    public String[] getJaxbPackagesToScan() {
        return jaxbPackagesToScan;
    }

    public void setJaxbPackagesToScan(String[] jaxbPackagesToScan) {
        this.jaxbPackagesToScan = jaxbPackagesToScan;
    }

    public Unmarshaller getUnmarshaller() {
        if (unmarshaller == null) {
            unmarshaller = new Jaxb2Marshaller();

            if (StringUtils.hasLength(jaxbContextPath)) {
                ((Jaxb2Marshaller) unmarshaller).setContextPath(jaxbContextPath);
            }

            if (!ObjectUtils.isEmpty(jaxbClassesToBeBound)) {
                ((Jaxb2Marshaller) unmarshaller).setClassesToBeBound(jaxbClassesToBeBound);
            }

            if (!ObjectUtils.isEmpty(jaxbPackagesToScan)) {
                ((Jaxb2Marshaller) unmarshaller).setPackagesToScan(jaxbPackagesToScan);
            }
        }

        return unmarshaller;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        if (resourceBeanMapper == null) {
            resourceBeanMapper = new JdomResourceBeanMapper(getUnmarshaller());
        }

        return resourceBeanMapper;
    }

}
