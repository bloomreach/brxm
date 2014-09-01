/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id: TranslationUtils.java 163480 2013-05-06 09:19:16Z mmilicevic $"
 */
public final class TranslationUtils {

    private static Logger log = LoggerFactory.getLogger(TranslationUtils.class);

    private TranslationUtils() {
    }

    /**
     * @param node
     * @param property
     * @param language
     * @param message
     * @return
     * @throws RepositoryException
     */
    public static Node setTranslationForNode(final Node node, final String property, final String language, final String message) throws RepositoryException {
        Node translation = TranslationUtils.getMatchingTranslationNodeFromNode(node, property, language);
        if (translation == null) {
            translation = TranslationUtils.addTranslationNode(node, property, language);
        }
        setTranslationMessage(translation, message);
        return translation;
    }


    /**
     * @param node
     * @param language
     * @param property
     * @return
     * @throws RepositoryException
     * @throws NullPointerException when the node is null
     */
    private static Node addTranslationNode(final Node node, final String property, final String language) throws RepositoryException {
        final Node translation = node.addNode(HippoNodeType.HIPPO_TRANSLATION, HippoNodeType.HIPPO_TRANSLATION);
        if (StringUtils.isNotBlank(language)) {
            translation.setProperty(HippoNodeType.HIPPO_LANGUAGE, language);
        } else {
            translation.setProperty(HippoNodeType.HIPPO_LANGUAGE, StringUtils.EMPTY);
        }
        if (StringUtils.isNotBlank(property)) {
            translation.setProperty(HippoNodeType.HIPPO_PROPERTY, property);
        }
        return translation;
    }

    /**
     * @param node
     * @param message
     * @throws RepositoryException
     * @throws NullPointerException when the node is null
     */
    private static void setTranslationMessage(final Node node, final String message) throws RepositoryException {
        if (message == null) {
            node.setProperty(HippoNodeType.HIPPO_MESSAGE, StringUtils.EMPTY);
        } else {
            node.setProperty(HippoNodeType.HIPPO_MESSAGE, message);
        }
    }

    /**
     * @param node
     * @param language
     * @param property
     * @return
     * @throws RepositoryException
     */
    private static Node getMatchingTranslationNodeFromNode(final Node node, final String property, final String language) throws RepositoryException {
        if (node == null) {
            log.debug("No node to get translation node from");
            return null;
        }

        final NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            final Node child = nodeIterator.nextNode();
            if (!child.isNodeType(HippoNodeType.HIPPO_TRANSLATION)) {
                // continue when not a translation node
                continue;
            }

            final String childLanguage = getHippoLanguage(child);
            if (StringUtils.isNotBlank(language) && (!language.equals(childLanguage))) {
                continue;
            } else if (StringUtils.isNotBlank(childLanguage)) {
                continue;
            }
            final String childProperty = getHippoProperty(child);
            if (StringUtils.isNotBlank(property) && (!property.equals(childProperty))) {
                continue;
            } else if (StringUtils.isNotBlank(childProperty)) {
                continue;
            }

            // A match: return child
            return child;
        }
        // No match: return null
        return null;
    }

    /**
     * Try to get the {@code HippoNodeType.HIPPO_LANGUAGE} property from a node. Or null when the property does not
     * exist.
     *
     * @param node the node to get the language from
     * @return the value of the {@code HippoNodeType.HIPPO_LANGUAGE} property or null
     * @throws RepositoryException  when errors occur while accessing the repository
     * @throws NullPointerException when the node is null
     */
    public static String getHippoLanguage(final Node node) throws RepositoryException {
        return HippoNodeUtils.getStringProperty(node, HippoNodeType.HIPPO_LANGUAGE);
    }

    /**
     * Try to get the {@code HippoNodeType.HIPPO_PROPERTY} property from a node. Or null when the property does not
     * exist.
     *
     * @param node the node to get the property from
     * @return the value of the {@code HippoNodeType.HIPPO_PROPERTY} property or null
     * @throws NullPointerException when the node is null
     * @throws RepositoryException  when errors occur while accessing the repository
     */
    public static String getHippoProperty(final Node node) throws RepositoryException {
        return HippoNodeUtils.getStringProperty(node, HippoNodeType.HIPPO_PROPERTY);
    }

    /**
     * Try to get the {@code HippoNodeType.HIPPO_MESSAGE} property from a node. Or null when the property does not
     * exist.
     *
     * @param node the node to get the message from
     * @return the value of the {@code HippoNodeType.HIPPO_MESSAGE} property or null
     * @throws NullPointerException when the node is null
     * @throws RepositoryException  when errors occur while accessing the repository
     */
    public static String getHippoMessage(final Node node) throws RepositoryException {
        return HippoNodeUtils.getStringProperty(node, HippoNodeType.HIPPO_MESSAGE);
    }

    public static List<Node> getTranslationsFromNode(final Node node) throws RepositoryException {
        return getTranslationsFromNode(node, null);
    }

    /**
     * @param node
     * @param property
     * @return
     * @throws RepositoryException
     */
    public static List<Node> getTranslationsFromNode(final Node node, final String property) throws RepositoryException {
        final List<Node> translations = new ArrayList<>();
        if (node == null) {
            log.debug("No node to get translations from");
            return Collections.emptyList();
        }

        final NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            final Node child = nodeIterator.nextNode();
            if (!child.isNodeType(HippoNodeType.HIPPO_TRANSLATION)) {
                // continue when not a translation node
                continue;
            }

            final String childProperty = getHippoProperty(child);
            if (StringUtils.isNotBlank(property) && (!property.equals(childProperty))) {
                continue;

            }

            //TODO check this statment:
            /*
            else if (StringUtils.isNotBlank(childProperty)) {
                //continue;
            }
            */

            // A match: add child to list
            translations.add(child);
        }
        return translations;
    }


}
