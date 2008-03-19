/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific lang governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.spi.Name;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hippoecm.repository.api.HippoNodeType;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class PhysicalExportSystemViewIssueTest603 extends FacetedNavigationAbstractTest {

    private static final String FACETSELECTNODE = "facetselect";
    private static final String FACETSEARCHNODE = "facetsearch";

    @Override
    protected Node commonStart() throws RepositoryException {
        numDocs = 10;
        super.commonStart();
        addFacetSelect();
        addFacetSearch();
        session.save();
        return null;
    }

    
    @Override
    public void testPerformance() throws RepositoryException, IOException {
    }

    public void testFacetSearchExport() throws RepositoryException, Exception {
        commonStart();
        Result result = new Result();
        ContentHandler handler = new ContentHandlerImpl(result, new String[] {FACETSEARCHNODE} );
        this.session.exportSystemView("/"+FACETSEARCHNODE, handler, true, false);
        if(verbose) {
            printSystemView("/"+FACETSEARCHNODE);
        }
        // if issue solved, assertTrue
        assertFalse(result.isSucces());
    }

    
    public void testFacetSelectExport() throws RepositoryException, Exception {
        commonStart();
        Result result = new Result();
        ContentHandler handler = new ContentHandlerImpl(result,  new String[] {FACETSELECTNODE});
        this.session.exportSystemView("/"+FACETSELECTNODE, handler, true, false);
        if(verbose) {
            printSystemView("/"+FACETSELECTNODE);
        }
        // if issue solved, assertTrue
        assertFalse(result.isSucces());
    }

    public void testTotalExport() throws RepositoryException, Exception {
        commonStart();
        Result result = new Result();
        ContentHandler handler = new ContentHandlerImpl(result, new String[] {FACETSELECTNODE, FACETSELECTNODE});
        this.session.exportSystemView("/", handler, true, false);
        if(verbose) {
        printSystemView("/");
        }
        // if issue solved, assertTrue
        assertFalse(result.isSucces());
    }

    private void addFacetSearch() throws RepositoryException {
        Node facetsearchNode = session.getRootNode().addNode(FACETSEARCHNODE, HippoNodeType.NT_FACETSEARCH);
        facetsearchNode.setProperty(HippoNodeType.HIPPO_QUERYNAME, "xyz");
        facetsearchNode.setProperty(HippoNodeType.HIPPO_DOCBASE, "/documents");
        facetsearchNode.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "x", "y", "z" });
    }

    private void addFacetSelect() throws RepositoryException {
        Node facetselectNode = session.getRootNode().addNode(FACETSELECTNODE, HippoNodeType.NT_FACETSELECT);
        facetselectNode.setProperty("hippo:docbase", "/documents");
        facetselectNode.setProperty("hippo:facets", new String[] {});
        facetselectNode.setProperty("hippo:values", new String[] {});
        facetselectNode.setProperty("hippo:modes", new String[] {});

    }

    class Result {
        boolean succes = true;

        public boolean isSucces() {
            return succes;
        }

        public void setSucces(boolean succes) {
            this.succes = succes;
        }

    }

    class ContentHandlerImpl implements ContentHandler {
        private Result result;
        private boolean noMoreNodes = false;
        private Set<String> noNodesBelowName;
        
        public ContentHandlerImpl(Result result, String[] noNodesBelowName) {
            this.result = result;
            this.noNodesBelowName = new HashSet<String>();
            List<String> list = Arrays.asList(noNodesBelowName);
            this.noNodesBelowName.addAll(list);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {

        }

        public void endDocument() throws SAXException {
        }

        

        public void endPrefixMapping(String prefix) throws SAXException {

        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

        }

        public void processingInstruction(String target, String data) throws SAXException {

        }

        public void setDocumentLocator(Locator locator) {

        }

        public void skippedEntity(String name) throws SAXException {

        }

        public void startDocument() throws SAXException {

        }

        public void endElement(String uri, String localName, String name) throws SAXException {
            String ns = Name.NS_SV_URI;
            if(ns.equals(uri) && "node".equals(localName)) {
                noMoreNodes = false;
            }
        }
        
        public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
            String ns = Name.NS_SV_URI;
            if(noMoreNodes && ns.equals(uri) && "node".equals(localName)) {
                result.setSucces(false);
            }
            if(ns.equals(uri) && "node".equals(localName) && noNodesBelowName.contains(atts.getValue(ns, "name"))){
                noMoreNodes = true;
            } 
            if (HippoNodeType.HIPPO_UUID.equals(atts.getValue(ns, "name"))) {
                /*
                 * a hippo uuid indicates virtual node: the export did export virtual nodes,
                 * hence the physical export is not correct
                 */
                //result.setSucces(false);
            }
            if (HippoNodeType.HIPPO_COUNT.equals(atts.getValue(ns, "name"))) {
                /*
                 * a hippo count indicates virtual node: the export did export virtual nodes,
                 * hence the physical export is not correct
                 */
                //result.setSucces(false);
            }
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {

        }

    }

    private void printSystemView(String path) throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.session.exportSystemView(path, out, true, false);  
        String export = prettyPrint(out.toByteArray());
        System.out.println(export);
    }
    
    private String prettyPrint(byte[] bytes) throws Exception {
        Source source = new StreamSource(new ByteArrayInputStream(bytes));
        DOMResult result = new DOMResult();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer identityTransformer = transformerFactory.newTransformer();
        identityTransformer.transform(source, result);
        org.w3c.dom.Document doc = (org.w3c.dom.Document) result.getNode();

        OutputFormat format = new OutputFormat(doc);
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setLineWidth(80);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLSerializer xmlSerializer = new XMLSerializer(out, format);
        xmlSerializer.serialize(doc);
        return out.toString("UTF-8");
    }
}
