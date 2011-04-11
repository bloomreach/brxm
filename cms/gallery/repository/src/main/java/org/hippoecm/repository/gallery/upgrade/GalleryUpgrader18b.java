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
package org.hippoecm.repository.gallery.upgrade;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class GalleryUpgrader18b implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(GalleryUpgrader18b.class);

    @Override
    public void register(UpdaterContext updaterContext) {
        updaterContext.registerName("v18b-cms-gallery-updater");
        updaterContext.registerStartTag("v18-cms-gallery");
        updaterContext.registerEndTag("v18b-cms-gallery");

        updateImageEditor(updaterContext);
        updateRichTextImagePicker(updaterContext);
        updateImageGallery(updaterContext);
    }


    private void updateImageEditor(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces/hippogallery/image/editor:templates/_default_"){
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node cropPluginNode = node.addNode("crop", "frontend:plugin");
                cropPluginNode.setProperty("gallery.processor.id", "service.gallery.processor");
                cropPluginNode.setProperty("mode", "${mode}");
                cropPluginNode.setProperty("plugin.class", "org.hippoecm.frontend.plugins.gallery.editor.ImageCropPlugin");
                cropPluginNode.setProperty("wicket.id", "${cluster.id}.crop");
                cropPluginNode.setProperty("wicket.model", "${wicket.model}");

                Node displayPluginNode = node.getNode("display");
                displayPluginNode.setProperty("wicket.id", "${cluster.id}.display");

                Node revertPluginNode = node.addNode("revert", "frontend:plugin");
                revertPluginNode.setProperty("gallery.processor.id", "service.gallery.processor");
                revertPluginNode.setProperty("mode", "${mode}");
                revertPluginNode.setProperty("plugin.class", "org.hippoecm.frontend.plugins.gallery.editor.ImageRegeneratePlugin");
                revertPluginNode.setProperty("wicket.id", "${cluster.id}.revert");
                revertPluginNode.setProperty("wicket.model", "${wicket.model}");

                Node rootNode = node.getNode("root");
                rootNode.setProperty("extension.crop", "${cluster.id}.crop");
                rootNode.setProperty("extension.display", "${cluster.id}.display");
                rootNode.setProperty("extension.revert", "${cluster.id}.revert");
                rootNode.setProperty("extension.upload", "${cluster.id}.upload");
                rootNode.setProperty("plugin.class", "org.hippoecm.frontend.plugins.gallery.editor.ThumbnailEditorButtons");
                rootNode.setProperty("wicket.extensions", new String[]{"extension.display", "extension.upload", "extension.crop", "extension.revert"});

                Node uploadPluginNode = node.getNode("upload");
                uploadPluginNode.setProperty("wicket.id", "${cluster.id}.upload");


            }
        });
    }

    private void updateRichTextImagePicker(final UpdaterContext context) {
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces/hippostd/html/editor:templates/_default_/root/Xinha.plugins.InsertImage"){
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Property property = node.getProperty("preferred.resource.names");
                if(property.isMultiple() && property.getValues().length > 1){
                    return;
                }
                else{
                    if(property.isMultiple()){
                        Value[] values = property.getValues();
                        if(values.length == 1 && "hippogallery:picture".equals(values[0].getString())){
                            node.setProperty("preferred.resource.names", new String[]{"hippogallery:original"});
                        }
                    }
                    else{
                         if("hippogallery:picture".equals(property.getString())){
                            node.setProperty("preferred.resource.names", "hippogallery:original");
                        }
                    }
                }
            }
        });
    }

    private void updateImageGallery(final UpdaterContext context){
      context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces/hippogallery/imageset"){
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                Node translationNodeEnOriginal = node.addNode("hippo:translation", "hippo:translation");
                translationNodeEnOriginal.setProperty("hippo:message", "Original");
                translationNodeEnOriginal.setProperty("hippo:language", "en");
                translationNodeEnOriginal.setProperty("hippo:property", "hippogallery:original");

                Node translationNodeEnThumbnail = node.addNode("hippo:translation", "hippo:translation");
                translationNodeEnThumbnail.setProperty("hippo:message", "Thumbnail");
                translationNodeEnThumbnail.setProperty("hippo:language", "en");
                translationNodeEnThumbnail.setProperty("hippo:property", "hippogallery:thumbnail");

                Node translationNodeFrOriginal = node.addNode("hippo:translation", "hippo:translation");
                translationNodeFrOriginal.setProperty("hippo:message", "Original [fr]");
                translationNodeFrOriginal.setProperty("hippo:language", "fr");
                translationNodeFrOriginal.setProperty("hippo:property", "hippogallery:original");

                Node translationNodeFrThumbnail = node.addNode("hippo:translation", "hippo:translation");
                translationNodeFrThumbnail.setProperty("hippo:message", "Thumbnail [fr]");
                translationNodeFrThumbnail.setProperty("hippo:language", "fr");
                translationNodeFrThumbnail.setProperty("hippo:property", "hippogallery:thumbnail");

            }
        });
    }



}

