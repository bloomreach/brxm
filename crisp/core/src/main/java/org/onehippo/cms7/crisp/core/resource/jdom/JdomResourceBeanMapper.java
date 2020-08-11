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

import java.io.IOException;

import org.jdom2.transform.JDOMSource;
import org.onehippo.cms7.crisp.api.resource.AbstractResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * Mapper to convert a {@link JdomResource} object to a bean.
 */
public class JdomResourceBeanMapper extends AbstractResourceBeanMapper {

    private final Unmarshaller unmarshaller;

    public JdomResourceBeanMapper(final Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    @Override
    public <T> T map(Resource resource, Class<T> beanType) throws ResourceException {
        if (!(resource instanceof JdomResource)) {
            throw new ResourceException("Cannot convert resource because it's not a JdomResource.");
        }

        try {
            return (T) unmarshaller.unmarshal(new JDOMSource(((JdomResource) resource).getJdomElement()));
        } catch (XmlMappingException e) {
            throw new ResourceException("XML Mapping Exception occurred. " + e, e);
        } catch (IOException e) {
            throw new ResourceException("IO Exception occurred. " + e, e);
        }
    }

}
