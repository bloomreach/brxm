/**
 * Copyright 2012-2021 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.mock.core.component;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.util.KeyValue;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * MockHstResponse
 */
public class MockHstResponse extends MockHstResponseBase implements HstResponse {

    private static final String [] EMPTY_STRING_ARRAY = new String[0];

    private String namespace;
    private List<KeyValue<String, Element>> headElements = new LinkedList<KeyValue<String, Element>>();
    private List<Element> processedElements = new ArrayList<>();
    private Map<String, String []> renderParameters;
    private String renderPath;
    private String serveResourcePath;
    private String forwardPathInfo;
    private Element wrapperElement;
    private List<Comment> preambleComments = new ArrayList<Comment>();
    private List<Element> preambleElements = new ArrayList<Element>();
    private List<Comment> epilogueComments = new ArrayList<Comment>();
    private boolean rendererSkipped;

    public HstURL createRenderURL() {
        MockHstURL url = new MockHstURL();
        url.setType(HstURL.RENDER_TYPE);
        return url;
    }

    public HstURL createNavigationalURL(String pathInfo) {
        MockHstURL url = new MockHstURL();
        url.setType(HstURL.RENDER_TYPE);
        // TODO deal with pathInfo
        return url;
    }

    public HstURL createActionURL() {
        MockHstURL url = new MockHstURL();
        url.setType(HstURL.ACTION_TYPE);
        return url;
    }

    public HstURL createResourceURL() {
        MockHstURL url = new MockHstURL();
        url.setType(HstURL.RESOURCE_TYPE);
        return url;
    }

    public HstURL createResourceURL(String referenceNamespace) {
        MockHstURL url = new MockHstURL();
        url.setType(HstURL.RESOURCE_TYPE);
        // TODO deal with referenceNamespace
        return url;
    }

    public HstURL createComponentRenderingURL() {
        MockHstURL url = new MockHstURL();
        url.setType(HstURL.COMPONENT_RENDERING_TYPE);
        return url;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setRenderParameter(String key, String value) {
        if (value == null) {
            setRenderParameter(key, EMPTY_STRING_ARRAY);
        } else {
            setRenderParameter(key, new String[] { value });
        }
    }

    public void setRenderParameter(String key, String[] values) {
        if (this.renderParameters == null) {
            this.renderParameters = new HashMap<String, String[]>();
        }

        if (values == null) {
            this.renderParameters.remove(key);
        } else {
            this.renderParameters.put(key, values);
        }
    }

    public void setRenderParameters(Map<String, String[]> parameters) {
        if (parameters == null) {
            this.renderParameters = null;
        } else {
            if (this.renderParameters == null) {
                this.renderParameters = new HashMap<String, String[]>();
            } else {
                this.renderParameters.clear();
            }

            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                setRenderParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    public Map<String, String[]> getRenderParameters() {
        return this.renderParameters;
    }

    public Element createElement(String tagName) {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            dbfac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            return doc.createElement(tagName);
        } catch (ParserConfigurationException e) {
            throw new DOMException((short) 0, "Initialization failure");
        }
    }

    public Comment createComment(String comment) {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        try {
            dbfac.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbfac.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            return doc.createComment(comment);
        } catch (ParserConfigurationException e) {
            throw new DOMException((short) 0, "Initialization failure");
        }
    }

    public void addHeadElement(Element element, String keyHint) {
        if (element == null) {
            if (keyHint != null) {
                KeyValue<String, Element> kvPair = new MockKeyValue<String, Element>(keyHint, null, true);
                headElements.remove(kvPair);
            } else {
                // If element is null and keyHint is null, remove all head elements.
                headElements.clear();
            }

            return;
        }

        KeyValue<String, Element> kvPair = new MockKeyValue<String, Element>(keyHint, element, true);

        if (!headElements.contains(kvPair)) {
            headElements.add(kvPair);
        }
    }

    public List<Element> getHeadElements() {
        List<Element> elements = new LinkedList<Element>();

        if (headElements != null) {
            for (KeyValue<String, Element> kv : headElements) {
                elements.add(kv.getValue());
            }
        }

        return elements;
    }

    public boolean containsHeadElement(String keyHint) {
        boolean containing = false;

        if (headElements != null && keyHint != null) {
            KeyValue<String, Element> kvPair = new MockKeyValue<String, Element>(keyHint, null, true);
            containing = this.headElements.contains(kvPair);
        }

        return containing;
    }

    @Override
    public void addProcessedHeadElement(final Element headElement) {
        processedElements.add(headElement);
    }

    public List<Element> getProcessedHeadElements() {
        return processedElements;
    }

    public void setRenderPath(String renderPath) {
        this.renderPath = renderPath;
    }

    public String getRenderPath() {
        return renderPath;
    }

    public void setServeResourcePath(String serveResourcePath) {
        this.serveResourcePath = serveResourcePath;
    }

    public String getServeResourcePath() {
        return serveResourcePath;
    }

    public void flushChildContent(String name) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void flushChildContent(final String name, final Writer writer) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public List<String> getChildContentNames() {
        return Collections.emptyList();
    }

    public void forward(String pathInfo) throws IOException {
        forwardPathInfo = pathInfo;
    }

    public String getForwardPathInfo() {
        return forwardPathInfo;
    }

    public void setWrapperElement(Element element) {
        wrapperElement = element;
    }

    public Element getWrapperElement() {
        return wrapperElement;
    }

    public void addPreamble(Comment comment) {
        preambleComments.add(comment);
    }

    public void addPreamble(Element element) {
        preambleElements.add(element);
    }

    @Override
    public List<Node> getPreambleNodes() {
        if (preambleComments == null && preambleElements == null) {
            return Collections.emptyList();
        }
        List<Node> preambleNodes = new LinkedList<>();
        if (preambleComments != null) {
            preambleNodes.addAll(preambleComments);
        }
        if (preambleElements != null) {
            preambleNodes.addAll(preambleElements);
        }
        return Collections.unmodifiableList(preambleNodes);
    }

    public void addEpilogue(Comment comment) { epilogueComments.add(comment); }

    @Override
    public List<Node> getEpilogueNodes() {
        if (epilogueComments != null) {
            return Collections.unmodifiableList(epilogueComments);
        }
        return Collections.emptyList();
    }

    public List<Comment> getPreambleComments() {
        return preambleComments;
    }

    public List<Element> getPreambleElements() {
        return preambleElements;
    }

    public List<Comment> getEpilogueComments() { return epilogueComments; }

    public boolean isRendererSkipped() {
        return rendererSkipped;
    }

    public void setRendererSkipped(boolean rendererSkipped) {
        this.rendererSkipped = rendererSkipped;
    }

    private class MockKeyValue<K, V> implements KeyValue<K, V> {

        private K key;
        private V value;
        private boolean compareByKeyOnly;

        /**
         * Constructs a new pair with the specified key and given value.
         *
         * @param key
         *            the key for the entry, may be null
         * @param value
         *            the value for the entry, may be null
         */
        public MockKeyValue(final K key, final V value) {
            this(key, value, false);
        }

        /**
         * Constructs a new pair with the specified key and given value.
         *
         * @param key
         *            the key for the entry, may be null
         * @param value
         *            the value for the entry, may be null
         * @param compareByKeyOnly
         *            flag if equals() depends on key only
         */
        public MockKeyValue(final K key, final V value, final boolean compareByKeyOnly) {
            this.key = key;
            this.value = value;
            this.compareByKeyOnly = compareByKeyOnly;
        }

        /**
         * Constructs a new pair from the specified <code>KeyValue</code>.
         *
         * @param pair
         *            the pair to copy, must not be null
         * @throws NullPointerException
         *             if the entry is null
         */
        public MockKeyValue(final KeyValue<K, V> pair) {
            this(pair, false);
        }

        /**
         * Constructs a new pair from the specified <code>KeyValue</code>.
         *
         * @param pair
         *            the pair to copy, must not be null
         * @param compareByKeyOnly
         *            flag if equals() depends on key only
         * @throws NullPointerException
         *             if the entry is null
         */
        public MockKeyValue(final KeyValue<K, V> pair, final boolean compareByKeyOnly) {
            this(pair.getKey(), pair.getValue(), compareByKeyOnly);
        }

        /**
         * Constructs a new pair from the specified <code>Map.Entry</code>.
         *
         * @param entry
         *            the entry to copy, must not be null
         * @throws NullPointerException
         *             if the entry is null
         */
        public MockKeyValue(final Map.Entry<K, V> entry) {
            this(entry, false);
        }

        /**
         * Constructs a new pair from the specified <code>Map.Entry</code>.
         *
         * @param entry
         *            the entry to copy, must not be null
         * @param compareByKeyOnly
         *            flag if equals() depends on key only
         * @throws NullPointerException
         *             if the entry is null
         */
        public MockKeyValue(final Map.Entry<K, V> entry, final boolean compareByKeyOnly) {
            this(entry.getKey(), entry.getValue(), compareByKeyOnly);
        }

        /**
         * Gets the key from the pair.
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * Gets the value from the pair.
         *
         * @return the value
         */
        public V getValue() {
            return value;
        }

        // -----------------------------------------------------------------------
        /**
         * Sets the key.
         *
         * @param key
         *            the new key
         * @return the old key
         * @throws IllegalArgumentException
         *             if key is this object
         */
        public K setKey(final K key) {
            if (key == this) {
                throw new IllegalArgumentException("DefaultKeyValue may not contain itself as a key.");
            }

            final K old = this.key;
            this.key = key;

            return old;
        }

        /**
         * Sets the value.
         *
         * @return the old value of the value
         * @param value
         *            the new value
         * @throws IllegalArgumentException
         *             if value is this object
         */
        public V setValue(final V value) {
            if (value == this) {
                throw new IllegalArgumentException("DefaultKeyValue may not contain itself as a value.");
            }

            final V old = this.value;
            this.value = value;

            return old;
        }

        // -----------------------------------------------------------------------
        /**
         * Compares this <code>KeyValue</code> with another <code>KeyValue</code>.
         * <p> Returns true if the compared object is also a <code>DefaultKeyValue</code>,
         * and its key and value are equal to this object's key and value.
         *
         * @param obj
         *            the object to compare to
         * @return true if equal key and value
         */
        @SuppressWarnings("unchecked")
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof MockKeyValue == false) {
                return false;
            }

            MockKeyValue<K, V> other = (MockKeyValue<K, V>) obj;

            if (this.compareByKeyOnly) {
                return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()));
            } else {
                return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()))
                        && (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
            }
        }

        /**
         * Gets a hashCode compatible with the equals method.
         *
         * @return a suitable hash code
         */
        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
        }

    }

}
