/*
 *  Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.mock.resource;

import java.io.IOException;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.core.resource.jdom.JdomResource;
import org.onehippo.cms7.crisp.core.resource.jdom.JdomResourceBeanMapper;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.xml.XMLConstants;

/**
 * Adapter class for XML / JDOM based resource data, to be used in unit tests by mocking.
 */
public class MockJdomResourceResolverAdapter extends AbstractMockResourceResolverAdapter {

    private static final Unmarshaller DEFAULT_UNMARSHALLER = new Jaxb2Marshaller();

    private Unmarshaller unmarshaller;

    @Override
    public ResourceBeanMapper getResourceBeanMapper() throws ResourceException {
        ResourceBeanMapper mapper = super.getResourceBeanMapper();

        if (mapper != null) {
            return mapper;
        }

        return new JdomResourceBeanMapper(getUnmarshaller());
    }

    public Unmarshaller getUnmarshaller() {
        if (unmarshaller == null) {
            return DEFAULT_UNMARSHALLER;
        }

        return unmarshaller;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    @Override
    protected Resource inputToResource(final InputStream inputStream) throws IOException {
        try {
            SAXBuilder jdomBuilder = new SAXBuilder();
            jdomBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            jdomBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = jdomBuilder.build(inputStream);
            final Element elem = document.getRootElement();
            return new JdomResource(elem);
        } catch (JDOMException e) {
            throw new ResourceException("Failed to parse input to jdom.", e);
        }
    }

}
