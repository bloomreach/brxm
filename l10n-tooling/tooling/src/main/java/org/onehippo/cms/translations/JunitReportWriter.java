/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.translations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

public class JunitReportWriter {
    
    private final TestSuite suite = new TestSuite("Translations Update");
    
    public void startTestCase(final String name) {
        suite.addTestCase(name);
    }
    
    public void failure(final String message) {
        suite.failure(message);
    }
    
    public void error(final String message) {
        suite.error(message);
    }

    public void write(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            JAXBContext context = JAXBContext.newInstance(TestSuite.class);
            TransformerHandler handler = ((SAXTransformerFactory)SAXTransformerFactory.newInstance()).newTransformerHandler();
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty("method", "xml");
            transformer.setOutputProperty("encoding", "UTF-8");
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            handler.setResult(new StreamResult(writer));
            context.createMarshaller().marshal(suite, handler);
        } catch (JAXBException | TransformerConfigurationException var9) {
            throw new IOException("Failed to serialize the report.", var9);
        }
    }

    
    @XmlRootElement(name = "testsuite")
    public static class TestSuite {
        @XmlAttribute
        public String name;
        @XmlAttribute(name = "failures")
        public int failureCount;
        @XmlAttribute(name = "errors")
        public int errorCount;
        @XmlElement(name = "testcase")
        public Collection<TestCase> testCases = new ArrayList<>();
        private TestCase currentTestCase;
        
        public TestSuite() {}
        
        private TestSuite(final String name) {
            this.name = name;
        }
        
        private void addTestCase(final String name) {
            currentTestCase = new TestCase(name);
            testCases.add(currentTestCase);
        }

        private void failure(final String message) {
            currentTestCase.addFailure(message);
            failureCount++;
        }

        private void error(final String message) {
            currentTestCase.addError(message);
            errorCount++;
        }

    }
    
    public static class TestCase {
        @XmlAttribute
        public String name;
        @XmlAttribute(name = "failures")
        public int failureCount;
        @XmlAttribute(name = "errors")
        public int errorCount;
        @XmlElement(name = "failure")
        public Collection<Failure> failures = new ArrayList<>();
        @XmlElement(name = "error")
        public Collection<Error> errors = new ArrayList<>();
        
        public TestCase() {}
        
        private TestCase(final String name) {
            this.name = name;
        }
        
        private void addFailure(final String message) {
            failures.add(new Failure(message));
            failureCount++;
        }
        
        private void addError(final String message) {
            errors.add(new Error(message));
            errorCount++;
        }
        
    }
    
    public static class Failure {
        @XmlAttribute
        public String message;

        public Failure() {}
        
        private Failure(final String message) {
            this.message = message;
        }
    }

    public static class Error {
        @XmlAttribute
        public String message;

        public Error() {}
        
        private Error(final String message) {
            this.message = message;
        }
    }
    
}
