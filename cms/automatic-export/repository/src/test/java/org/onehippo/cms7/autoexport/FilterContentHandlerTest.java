/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.autoexport;

import static org.custommonkey.xmlunit.DifferenceConstants.TEXT_VALUE_ID;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


public class FilterContentHandlerTest {
    
    private static final String OUTPUT_DIR;
    private static final String TEST_HOME;
    
    static {
        File basedir = new File(System.getProperty("user.dir"));
        if (basedir.getName().equals("target")) {
            OUTPUT_DIR = basedir.getPath() + "/filter";
            TEST_HOME = basedir.getParent() + "/src/test/resources/filter";
        } else {
            OUTPUT_DIR = basedir.getPath() + "/target/filter";
            TEST_HOME = basedir.getPath() + "/src/test/resources/filter";
        }
        new File(OUTPUT_DIR).mkdirs();
    }
    
    @Test
    public void testFilterSubContextPaths() throws Exception {
        doTestFilterSubContextPaths("/");
        doTestFilterSubContextPaths("/test");
    }

    private void doTestFilterSubContextPaths(String rootPath) throws Exception {
        // set up
        String prefix = rootPath.equals("/") ? "" : rootPath;
        File resultFile, expectedFile;
        File inputFile = new File(TEST_HOME, "input.xml");
        List<String> subContextPaths = null;
        Configuration configuration = new Configuration(true, null, new ExclusionContext(new ArrayList<String>()), new ArrayList<String>());

        // case 1
        resultFile = new File(OUTPUT_DIR, "result1.xml");
        subContextPaths = Arrays.asList(prefix + "/foo/foo");

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected1.xml");
        assertSimilar(expectedFile, resultFile);

        // case 2
        resultFile = new File(OUTPUT_DIR, "result2.xml");
        subContextPaths = Arrays.asList(prefix + "/foo/bar/bar");

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected2.xml");
        assertSimilar(expectedFile, resultFile);

        // case 3
        resultFile = new File(OUTPUT_DIR, "result3.xml");
        subContextPaths = Arrays.asList(prefix + "/foo/bar/quz/quz");

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected3.xml");
        assertSimilar(expectedFile, resultFile);
    }

    @Test
    public void testFilterUuids() throws Exception {
        doTestFilterUuids("/");
        doTestFilterUuids("/test");
    }

    private void doTestFilterUuids(String rootPath) throws Exception {
        // set up
        String prefix = rootPath.equals("/") ? "" : rootPath;
        File resultFile, expectedFile;
        File inputFile = new File(TEST_HOME, "input.xml");
        List<String> subContextPaths = Collections.emptyList();
        Configuration configuration = null;

        // case 1
        resultFile = new File(OUTPUT_DIR, "result4.xml");
        configuration = new Configuration(true, null, new ExclusionContext(new ArrayList<String>()), Arrays.asList(prefix + "/"));

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected4.xml");
        assertSimilar(expectedFile, resultFile);

        // case 2
        resultFile = new File(OUTPUT_DIR, "result5.xml");
        configuration = new Configuration(true, null, new ExclusionContext(new ArrayList<String>()), Arrays.asList(prefix + "/foo/foo/foo"));

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected5.xml");
        assertSimilar(expectedFile, resultFile);
    }

    @Test
    public void testFilterExclusionContext() throws Exception {
        doTestFilterExclusionContext("/");
        doTestFilterExclusionContext("/test");
    }

    private void doTestFilterExclusionContext(String rootPath) throws Exception {
        // set up
        String prefix = rootPath.equals("/") ? "" : rootPath;
        File resultFile, expectedFile;
        File inputFile = new File(TEST_HOME, "input.xml");
        List<String> subContextPaths = Collections.emptyList();
        List<String> filterUuidPaths = Collections.emptyList();
        ExclusionContext exclusionContext = null;
        Configuration configuration = null;

        // case 1
        resultFile = new File(OUTPUT_DIR, "result6.xml");
        exclusionContext = new ExclusionContext(Arrays.asList(prefix + "/foo/**"));
        configuration = new Configuration(true, null, exclusionContext, filterUuidPaths);

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected6.xml");
        assertSimilar(expectedFile, resultFile);

        // case 2
        resultFile = new File(OUTPUT_DIR, "result7.xml");
        exclusionContext = new ExclusionContext(Arrays.asList(prefix + "/foo/foo"));
        configuration = new Configuration(true, null, exclusionContext, filterUuidPaths);

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected7.xml");
        assertSimilar(expectedFile, resultFile);

        // case 3
        resultFile = new File(OUTPUT_DIR, "result8.xml");
        exclusionContext = new ExclusionContext(Arrays.asList(prefix + "/**/bar"));
        configuration = new Configuration(true, null, exclusionContext, filterUuidPaths);

        filter(inputFile, resultFile, rootPath, subContextPaths, configuration);

        expectedFile = new File(TEST_HOME, "expected8.xml");
        assertSimilar(expectedFile, resultFile);
    }

    private void filter(File inputFile, File outputFile, String rootPath, List<String> subContextPaths, Configuration configuration) throws IOException, TransformerConfigurationException, SAXException {
        FileReader inputFileReader = new FileReader(inputFile);
        InputSource inputFileSource = new InputSource(inputFileReader);
        
        FileWriter outputFileWriter = new FileWriter(outputFile);

        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler handler = stf.newTransformerHandler();
        Transformer transformer = handler.getTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
        handler.setResult(new StreamResult(outputFileWriter));

        FilterContentHandler filter = new FilterContentHandler(handler, rootPath, subContextPaths, configuration.getFilterUuidPaths(), configuration.getExclusionContext());
        
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(filter);
        reader.parse(inputFileSource);
    }
    
    private void assertSimilar(File expectedFile, File resultFile) throws SAXException, IOException {
        FileReader result = new FileReader(resultFile);
        FileReader expected = new FileReader(expectedFile);
        Diff diff = new Diff(expected, result);
        diff.overrideDifferenceListener(new IgnorableWhitespaceDifferenceListener());
        assertTrue(diff.similar());
    }

    private static class IgnorableWhitespaceDifferenceListener implements DifferenceListener {

        @Override
        public int differenceFound(Difference difference) {
            if (difference.getId() == TEXT_VALUE_ID) {
                Element parentElement = (Element) difference.getTestNodeDetail().getNode().getParentNode();
                if (parentElement.getNodeName().equals("sv:node")) {
                    String trimmedTestTextValue = parentElement.getTextContent().trim();
                    String trimmedControlTextValue = difference.getControlNodeDetail().getValue().trim();
                    if (trimmedTestTextValue.equals(trimmedControlTextValue)) {
                        return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                    }
                }
            }
            return RETURN_ACCEPT_DIFFERENCE;
        }

        @Override
        public void skippedComparison(Node arg0, Node arg1) {
        }
        
    }

}
