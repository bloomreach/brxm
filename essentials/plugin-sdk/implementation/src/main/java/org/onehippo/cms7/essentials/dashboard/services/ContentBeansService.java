/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.nio.file.Path;
import java.util.Map;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;

/**
 * ContentBeansService provides support for updating the project's (HST) content beans, to be used typically after
 * an action that altered one of more document types of your project.
 *
 * TODO: this API doesn't feel right yet, and therefore continues to live in the SDK implementation for the time being.
 * The problem mostly evolves around different representations of an image set: JCR nodetype, classname, bean path.
 * We should converge to a single imageset "ID" used consistently in the API, potentially augmented with an image set
 * data bean providing additional information about an image set to the user of the API.
 *
 * The current users, the BeanWriter and the GalleryManager, hence keep depending on the SDK implementation for now.
 */
public interface ContentBeansService {

    /**
     * Find all Hippo Beans (related to document types) of the project.
     *
     * @param context for accessing the project
     * @return        a mapping of (prefixed) JCR node name to bean class path
     */
    Map<String, Path> findBeans(PluginContext context);

    /**
     * (Re)create the content beans for all document types of the project's namespace.
     *
     * During the process, user-level feedback messages get populated.
     *
     * @param jcrService        for accessing the JCR repository
     * @param context           for accessing the project (namespace, beans path etc.)
     * @param feedback          to populate user feedback
     * @param imageSetClassName when specified, newly created getters for image links will return this type of images
     */
    void createBeans(JcrService jcrService, PluginContext context, UserFeedback feedback, String imageSetClassName);

    /**
     * Clean-up Essentials-generated getters if the corresponding document type field no longer exists.
     *
     * @param jcrService for accessing the JCR repository
     * @param context    for accessing the project
     * @param feedback   to populate user feedback
     */
    void cleanupMethods(JcrService jcrService, PluginContext context, UserFeedback feedback);

    /**
     * Retrieve a <classname, beanpath> map of the project's custom image sets,
     * augmented with 'HippoGalleryImageSet' -> null.
     *
     * @param context for accessing the project
     * @return map consisting of at least the default image set classname
     */
    Map<String, Path> getExistingImageTypes(PluginContext context);

    /**
     * Convert all image link getters of the project to return the specified image set class.
     *
     * @param jcrName  JCR name of the image set (as per annotation)
     * @param context  for accessing the project
     * @param feedback to populate user feedback
     */
    void convertImageMethods(String jcrName, PluginContext context, UserFeedback feedback);

    /**
     * Convert all image link getters of the project to return the specified image set class.
     *
     * @param classname classname of the image set
     * @param context   for accessing the project
     * @param feedback  to populate user feedback
     */
    void convertImageMethodsForClassname(String classname, PluginContext context, UserFeedback feedback);
}
