/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Iterables;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Utility methods to help with the correct indentation of added pieces of XML.
 */
public class Dom4JUtils {
    private static final Logger LOG = LoggerFactory.getLogger(Dom4JUtils.class);
    private static final int DEFAULT_INDENT_CHARS = 2;
    private static final String INDENT = "\n                                                                      ";
    private static final String CHILD_ELEMENTS = "./*";

    private Dom4JUtils() { }

    public static class ModifierException extends Exception {
        public ModifierException(final String message) {
            super(message);
        }
    }

    @FunctionalInterface
    public interface Modifier {
        void run(Document doc) throws ModifierException;
    }

    /**
     * Let a modifier update the content of an XML file
     *
     * @param xmlFile  the file to update
     * @param modifier logic to modify the dom4j document. Should throw a ModifierException if the modifier logic fails.
     * @return         true if the modifier was applied and potential changes were saved successfully, false otherwise.
     */
    public static boolean update(final File xmlFile, final Modifier modifier) {
        try (FileInputStream xmlStream = new FileInputStream(xmlFile)){
            SAXReader xmlReader = new SAXReader();
            xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            final Document doc = xmlReader.read(xmlStream);
            modifier.run(doc);
            final FileWriter writer = new FileWriter(xmlFile);
            doc.write(writer);
            writer.close();
            return true;
        } catch (ModifierException | SAXException e) {
            LOG.error(e.getMessage());
        } catch (DocumentException | IOException e) {
            LOG.error("Failed to update XML file '{}'.", xmlFile, e);
        }
        return false;
    }

    public static Element addIndentedElement(final Element parent, final String elementName) {
        return addIndentedElement(parent, elementName, null, DEFAULT_INDENT_CHARS, false);
    }

    public static Element addIndentedElement(final Element parent, final String elementName, final String elementText) {
        return addIndentedElement(parent, elementName, elementText, DEFAULT_INDENT_CHARS, false);
    }

    public static Element addIndentedSameNameSibling(final Element parent, final String elementName, final String elementText) {
        return addIndentedElement(parent, elementName, elementText, DEFAULT_INDENT_CHARS, true);
    }

    /**
     * Add a (child) element with correct indentation to the parent element.
     *
     * @param parent       parent element, linked into a {@link Document}.
     * @param elementName  name of the element
     * @param elementText  text of the element
     * @param indentSpaces number of space characters between indentation levels
     * @param appendToSameNameSiblings if true, append new element just after last siblings with same element name.
     *                                 if false, append new element as last child element.
     * @return             the created (child) element
     */
    public static Element addIndentedElement(final Element parent, final String elementName, final String elementText,
                                             final int indentSpaces, final boolean appendToSameNameSiblings) {
        final Element element = createElement(parent, elementName, elementText);

        addIndentedElement(parent, element, indentSpaces, appendToSameNameSiblings);

        return element;
    }

    /**
     * Try to insert a new child element before the child element matching the specified location selector
     *
     * If the location selector produces at least one match for the parent element, the first match is used to derive
     * the parent's child element, before which the new element is inserted.
     *
     * If the location selector produces no match, the new element is inserted nevertheless, as last same-name-sibling
     * if there already are same-name-siblings, or as the last child otherwise.
     *
     * @param parent           parent element for new element
     * @param elementName      name of the new element
     * @param locationSelector selector to locate before which existing child the new element is to be inserted
     * @return                 the new (child) element
     */
    public static Element insertIndentedElement(final Element parent, final String elementName, final String locationSelector) {
        final Element element = createElement(parent, elementName, null);

        Node insertBefore = Iterables.getFirst(parent.selectNodes(locationSelector), null);
        if (insertBefore != null) {
            final List<Node> children = parent.content();
            // Selected node may be nested child (depending on selector), determine immediate child node.
            while (!children.contains(insertBefore)) {
                insertBefore = insertBefore.getParent();
            }
            int insertionIndex = children.indexOf(insertBefore);
            if (insertionIndex > 0) {
                // If node before 'insertionIndex' is indentation, clone it for the new element
                Node indentation = children.get(insertionIndex - 1);
                if (indentation instanceof DefaultText) {
                    children.add(insertionIndex, (Node) indentation.clone());
                }
            }
            children.add(insertionIndex, element);
        } else {
            LOG.warn("Cannot insert element '{}' before selector '{}' for element '{}', no matching child found.",
                    elementName, locationSelector, parent.getPath());
            addIndentedElement(parent, element, DEFAULT_INDENT_CHARS, true);
        }

        return element;
    }

    /**
     * Add a (child) element with correct indentation to the parent element.
     *
     * @param parent parent element, linked into a {@link Document}.
     * @param child  child element, to be added.
     */
    public static void addIndentedElement(final Element parent, final Element child) {
        addIndentedElement(parent, child, DEFAULT_INDENT_CHARS, false);
    }

    private static Element createElement(final Element parent, final String elementName, final String elementText) {
        final QName elementQName = new QName(elementName, parent.getNamespace());
        final Element element = DocumentHelper.createElement(elementQName);
        if (elementText != null) {
            element.addText(elementText);
        }
        return element;
    }

    private static void addIndentedElement(final Element parent, final Element element, final int indentSpaces,
                                           final boolean appendToSameNameSiblings) {
        final List<Node> children = parent.content();
        if (children.isEmpty()) {
            final Node indentation = deriveIndentation(parent);
            if (indentation != null) {
                final String prefix = INDENT.substring(0, indentation.getText().length() + indentSpaces);

                parent.addText(prefix);
                parent.add(element);
                parent.add(indentation);
            } else {
                // Don't bother about indentation
                parent.add(element);
            }
        } else {
            Node lastElement = null;
            if (appendToSameNameSiblings) {
                final String selector = "./*[name()='" + element.getName() + "']";
                lastElement = Iterables.getLast(parent.selectNodes(selector), null);
            }
            if (lastElement == null) {
                lastElement = Iterables.getLast(parent.selectNodes(CHILD_ELEMENTS));
            }
            if (lastElement != null) {
                int lastElementIndex = children.indexOf(lastElement);
                final Node indentation = deriveIndentation(lastElement);
                if (indentation != null) {
                    children.add(++lastElementIndex, indentation);
                }
                children.add(++lastElementIndex, element);
            } else {
                children.add(element);
            }
        }
    }

    private static Node deriveIndentation(final Node element) {
        final Element parent = element.getParent();
        if (parent == null) {
            return null;
        }

        final List<Node> children = parent.content();
        final int index = children.indexOf(element);
        if (index > 0) {
            final Node prefixNode = children.get(index - 1);
            final String prefixText = prefixNode.getText();
            if (prefixNode instanceof DefaultText && prefixText.contains("\n")) {
                final int indentLength = prefixText.substring(prefixText.lastIndexOf('\n')).length();
                final Node indentation = (Node)prefixNode.clone();
                indentation.setText(INDENT.substring(0, indentLength));
                return indentation;
            }
        }

        return null;
    }
}
