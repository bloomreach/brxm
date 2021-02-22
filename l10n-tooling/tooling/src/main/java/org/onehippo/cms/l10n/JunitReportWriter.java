/*
 *  Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

public class JunitReportWriter {

    final Report report;

    public JunitReportWriter(final String moduleName) {
        report = new Report(moduleName);
    }

    public void startTestCase(final String name) {
        report.addTestCase(name);
    }

    public void failure(final String message) {
        report.failure(message);
    }

    public void error(final String message) {
        report.error(message);
    }

    public void write(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            final JAXBContext context = JAXBContext.newInstance(Report.class);
            // This is a compliant solution but it is still marked as an vulnerability issue by Sonar
            @SuppressWarnings("squid:S4435")
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            if (!factory.getFeature(SAXTransformerFactory.FEATURE)){
                throw new IOException("Factory does not support " + SAXTransformerFactory.FEATURE);
            }
            final TransformerHandler handler = ((SAXTransformerFactory) factory).newTransformerHandler();
            final Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty("method", "xml");
            transformer.setOutputProperty("encoding", "UTF-8");
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            handler.setResult(new StreamResult(writer));
            context.createMarshaller().marshal(report, handler);
        } catch (JAXBException | TransformerConfigurationException var9) {
            throw new IOException("Failed to serialize the report.", var9);
        }
    }

}
