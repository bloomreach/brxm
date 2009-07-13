/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.hippoecm.repository.api.HippoSession;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class XMLExport {
    SAXParserFactory factory;

    public XMLExport() {
        factory = SAXParserFactory.newInstance();
    }

    private void export(final Node node, File file, OutputStream ostream, boolean keepStructure) throws IOException, RepositoryException {
        try {
            final PipedOutputStream pstream = new PipedOutputStream();
            InputStream istream = new PipedInputStream(pstream);
            Thread outputThread = new Thread() {
                @Override
                public void run() {
                    try {
                        ((HippoSession)node.getSession()).exportDereferencedView(node.getPath(), pstream, false, false);
                        pstream.close();
                    } catch (IOException ex) {
                        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    } catch (RepositoryException ex) {
                        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                        ex.printStackTrace(System.err);
                    }
                }
            };
            outputThread.start();
            OutputProcessor processor;
            if (keepStructure && file != null) {
                XmlProcessor recordProcessor = new XmlProcessor(factory, file);
                recordProcessor.process();
                if(ostream == null) {
                    ostream = new FileOutputStream(file);
                }
                processor = new OutputProcessor(factory, recordProcessor, istream, ostream);
            } else {
                if(ostream == null) {
                    ostream = new FileOutputStream(file);
                }
                processor = new OutputProcessor(factory, istream, ostream);
            }
            processor.process();
            istream.close();
        } catch (SAXException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            throw new RepositoryException("Export exception", ex);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            throw new RepositoryException("Export exception", ex);
        }
    }
    public void export(Node node, File file) throws IOException, RepositoryException {
        export(node, file, null, true);
    }
    public void export(Node node, OutputStream ostream) throws IOException, RepositoryException {
        export(node, null, ostream, false);
    }
}

class OutputProcessor extends XmlProcessor {
    private int indent = 0;
    private StringBuffer textBuffer = null;
    boolean startOfDocument = true;
    PrintWriter out;
    boolean outputCopyright = false;

    OutputProcessor(SAXParserFactory factory, XmlProcessor recordProcessor, InputStream istream, OutputStream ostream) throws IOException, ParserConfigurationException, SAXException {
        super(factory, recordProcessor, istream);
        out = new PrintWriter(new OutputStreamWriter(ostream, "UTF8"));
    }

    OutputProcessor(SAXParserFactory factory, InputStream istream, OutputStream ostream) throws IOException, ParserConfigurationException, SAXException {
        super(factory, istream);
        out = new PrintWriter(new OutputStreamWriter(ostream, "UTF8"));
    }

    @Override
    void process() throws IOException, SAXException {
        startOfDocument = true;
        super.process();
        out.flush();
    }

    @Override
    public void startDocument()
            throws SAXException {
        super.startDocument();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        if (outputCopyright) {
            out.println("<!--");
            out.println("  Copyright 2007-2009 Hippo");
            out.println("");
            out.println("  Licensed under the Apache License, Version 2.0 (the  \"License\");");
            out.println("  you may not use this file except in compliance with the License.");
            out.println("  You may obtain a copy of the License at");
            out.println("");
            out.println("  http://www.apache.org/licenses/LICENSE-2.0");
            out.println("");
            out.println("  Unless required by applicable law or agreed to in writing, software");
            out.println("  distributed under the License is distributed on an \"AS IS\"");
            out.println("  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
            out.println("  See the License for the specific language governing permissions and");
            out.println("  limitations under the License.");
            out.println("-->");
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            out.println();
            out.flush();
        } finally {
            super.endDocument();
        }
    }

    private void flush() throws SAXException {
        if (textBuffer == null)
            return;
        String s = "" + textBuffer;
        s = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        out.print(s);
        textBuffer = null;
    }

    private void clear() {
        textBuffer = null;
    }

    @Override
    protected void insert(String literal) throws SAXException {
        if (startOfDocument)
            return;
        String whitespace = literals.get(new Location(Type.WHITESPACE, path, false));
        if (whitespace != null) {
            out.print(whitespace);
        }
        if (literal.trim().contains("\n")) {
            out.print("\n<!--");
            out.print(literal);
            out.print("-->\n\n");
        } else {
            for (int i = 0; i < indent; i++)
                out.print("  ");
            out.print("<!--");
            out.print(literal);
            out.print("-->\n");
        }
        whitespace = literals.get(new Location(Type.WHITESPACE, path, true));
        if (whitespace != null) {
            out.print(whitespace);
        }
    }

    @Override
    public void characters(char buf[], int offset, int len)
            throws SAXException {
        String s = new String(buf, offset, len);
        if (textBuffer == null) {
            textBuffer = new StringBuffer(s);
        } else {
            textBuffer.append(s);
        }
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
            throws SAXException {
        super.startElement(namespaceURI, sName, qName, attrs);
        startOfDocument = false;
        flush();
        String eName = sName;
        if ("".equals(eName))
            eName = qName;
        if (eName.equals("sv:value") && (path.endsWith("/hippo:paths") || path.endsWith("/jcr:uuid"))) {
            return;
        }
        for (int i = 0; i < indent; i++)
            out.print("  ");
        out.print("<" + eName);
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String aName = attrs.getLocalName(i);
                if ("".equals(aName))
                    aName = attrs.getQName(i);
                out.print(" ");
                out.print(aName + "=\"" + attrs.getValue(i) + "\"");
                if (aName.startsWith("xmlns:")) {
                    out.println();
                    out.print("        ");
                }
            }
        }
        out.print(">");
        if (!eName.equals("sv:value"))
            out.println();
        ++indent;
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName)
            throws SAXException {
        super.endElement(namespaceURI, sName, qName);
        String eName = sName;
        if ("".equals(eName))
            eName = qName;
        if (eName.equals("sv:value") && (path.endsWith("/hippo:paths") || path.endsWith("/jcr:uuid"))) {
            clear();
            return;
        }
        flush();
        --indent;
        if (!eName.equals("sv:value")) {
            for (int i = 0; i < indent; i++)
                out.print("  ");
        }
        out.println("</" + eName + ">");
    }
}

enum Type {
    COMMENT, WHITESPACE
}

class Location implements Comparable<Location> {
    private Type type;
    private String path;
    private boolean afterElement;

    public Location(Type type, String path, boolean afterElement) {
        this.type = type;
        this.path = path;
        this.afterElement = afterElement;
    }

    @Override
    public int compareTo(Location other) {
        int rtvalue = type.compareTo(other.type);
        if (rtvalue == 0) {
            if (path == null || other.path == null) {
                if (path == null && other.path == null)
                    return 0;
                else if (path == null)
                    return -1;
                else
                    return 1;
            }
            rtvalue = path.compareTo(other.path);
            if (rtvalue == 0) {
                if (afterElement == false) {
                    rtvalue = other.afterElement == false ? 0 : 1;
                } else {
                    rtvalue = other.afterElement == true ? 0 : -1;
                }
            }
        }
        return rtvalue;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Location) {
            Location other = (Location)o;
            return type == other.type && (path == null ? other.path == null : path.equals(other.path)) && afterElement == other.afterElement;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 29 * hash + (this.path != null ? this.path.hashCode() : 0);
        hash = 29 * hash + (this.afterElement ? 1 : 0);
        return hash;
    }
}

class XmlProcessor extends DefaultHandler2 {
    private StringBuffer commentBuffer = null;
    private StringBuffer whitespaceBuffer = null;
    protected String path = null;
    protected Map<Location, String> literals;
    private boolean playback;
    private SAXParser saxParser;
    private InputStream istream;

    XmlProcessor(SAXParserFactory factory, File file) throws IOException, ParserConfigurationException, SAXException {
        this(factory, new FileInputStream(file));
    }

    XmlProcessor(SAXParserFactory factory, InputStream istream) throws IOException, ParserConfigurationException, SAXException {
        this.istream = istream;
        saxParser = factory.newSAXParser();
        saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", this);
        literals = new TreeMap<Location, String>();
        playback = false;
    }

    protected XmlProcessor(SAXParserFactory factory, XmlProcessor recordProcessor, InputStream istream) throws IOException, ParserConfigurationException, SAXException {
        this.istream = istream;
        saxParser = factory.newSAXParser();
        literals = new TreeMap<Location, String>(recordProcessor.literals);
        playback = true;
    }

    void process() throws IOException, SAXException {
        try {
            saxParser.parse(istream, this);
        } catch (SAXException ex) {
            Throwable e = ex.getCause();
            if (e instanceof IOException) {
                throw (IOException)e;
            } else {
                throw ex;
            }
        }
        istream.close();
    }

    protected void insert(String literal) throws SAXException {
    }

    private void flush(boolean afterElement) throws SAXException {
        if (playback) {
            Location location = new Location(Type.COMMENT, path, afterElement);
            if (literals.containsKey(location)) {
                insert(literals.get(location));
            }
        } else {
            if (whitespaceBuffer != null) {
                literals.put(new Location(Type.WHITESPACE, path, (commentBuffer == null)), new String(whitespaceBuffer));
            }
            whitespaceBuffer = null;
            if (commentBuffer == null)
                return;
            String s = new String(commentBuffer);
            if (!s.trim().equals("")) {
                literals.put(new Location(Type.COMMENT, path, afterElement), s);
            }
            commentBuffer = null;
        }
    }

    @Override
    public void comment(char buf[], int offset, int len)
            throws SAXException {
        if (!playback) {
            String s = new String(buf, offset, len);
            if (commentBuffer == null) {
                commentBuffer = new StringBuffer(s);
            } else {
                commentBuffer.append(s);
            }
        }
    }

    @Override
    public void ignorableWhitespace(char[] buf, int offset, int len) {
        if (!playback) {
            String s = new String(buf, offset, len);
            if (whitespaceBuffer == null) {
                whitespaceBuffer = new StringBuffer(s);
            } else {
                whitespaceBuffer.append(s);
            }
        }
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
            throws SAXException {
        String eName = sName;
        if ("".equals(eName))
            eName = qName;
        if ("sv:property".equals(eName) || "property".equals(eName) ||
                "sv:node".equals(eName) || "node".equals(eName)) {
            if (attrs != null) {
                String name = "";
                for (int i = 0; i < attrs.getLength(); i++) {
                    String aName = attrs.getLocalName(i);
                    if ("".equals(aName))
                        aName = attrs.getQName(i);
                    if ("sv:name".equals(aName) || "name".equals(aName)) {
                        name = attrs.getValue(i);
                    }
                }
                if (path != null) {
                    path += "/" + name;
                } else {
                    path = name;
                }
            }
            flush(false);
        }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName)
            throws SAXException {
        String eName = sName;
        if ("".equals(eName))
            eName = qName;
        if ("sv:property".equals(eName) || "property".equals(eName) ||
                "sv:node".equals(eName) || "node".equals(eName)) {
            flush(true);
            int idx = path.lastIndexOf("/");
            if (idx < 0) {
                path = null;
            } else {
                path = path.substring(0, idx);
            }
        }
    }

    @Override
    public void startDocument() throws SAXException {
        flush(false);
    }

    @Override
    public void endDocument() throws SAXException {
        flush(true);
    }
}
