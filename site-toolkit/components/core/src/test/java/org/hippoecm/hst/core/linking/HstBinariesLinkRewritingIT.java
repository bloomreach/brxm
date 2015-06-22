/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;


import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HstBinariesLinkRewritingIT extends AbstractHstLinkRewritingIT {

    private HstRequestContext initContext() throws Exception {
        return getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
    }

    @Test
    public void binaries_link_for_existing_image_handle() throws Exception {
        // unit test content bootstraps 'unittestcontent/gallery/picture.jpeg'
        HstRequestContext requestContext = initContext();
        assertTrue(requestContext.getSession().nodeExists("/unittestcontent/gallery/picture.jpeg"));
        HstLink existingImage = linkCreator.create("/binaries/unittestcontent/gallery/picture.jpeg", requestContext.getResolvedMount().getMount());
        assertTrue(existingImage.isContainerResource());
        assertEquals("binaries/unittestcontent/gallery/picture.jpeg", existingImage.getPath());
        assertEquals("/site/binaries/unittestcontent/gallery/picture.jpeg" , existingImage.toUrlForm(requestContext, false));
    }

    @Test
    public void binaries_link_for_existing_image_document() throws Exception {
        // unit test content bootstraps 'unittestcontent/gallery/picture.jpeg'
        HstRequestContext requestContext = initContext();
        assertTrue(requestContext.getSession().nodeExists("/unittestcontent/gallery/picture.jpeg/picture.jpeg"));
        HstLink existingImage = linkCreator.create("/binaries/unittestcontent/gallery/picture.jpeg/picture.jpeg", requestContext.getResolvedMount().getMount());
        assertTrue(existingImage.isContainerResource());
        assertEquals("binaries/unittestcontent/gallery/picture.jpeg", existingImage.getPath());
        assertEquals("/site/binaries/unittestcontent/gallery/picture.jpeg" , existingImage.toUrlForm(requestContext, false));
    }

    @Test
    public void binaries_link_for_existing_image_primary_item_resource() throws Exception {
        // unit test content bootstraps 'unittestcontent/gallery/picture.jpeg'
        HstRequestContext requestContext = initContext();
        assertTrue(requestContext.getSession().nodeExists("/unittestcontent/gallery/picture.jpeg/picture.jpeg/hippogallery:original"));
        HstLink existingImagePrimaryResource = linkCreator.create("/binaries/unittestcontent/gallery/picture.jpeg/picture.jpeg/hippogallery:original", requestContext.getResolvedMount().getMount());
        assertTrue(existingImagePrimaryResource.isContainerResource());
        assertEquals("binaries/unittestcontent/gallery/picture.jpeg", existingImagePrimaryResource.getPath());
        assertEquals("/site/binaries/unittestcontent/gallery/picture.jpeg" , existingImagePrimaryResource.toUrlForm(requestContext, false));
    }

    @Test
    public void binaries_link_for_existing_image_NON_primary_item_resource() throws Exception {
        HstRequestContext requestContext = initContext();
        assertTrue(requestContext.getSession().nodeExists("/unittestcontent/gallery/picture.jpeg/picture.jpeg/hippogallery:thumbnail"));
        HstLink existingImageNonPrimaryResource = linkCreator.create("/binaries/unittestcontent/gallery/picture.jpeg/picture.jpeg/hippogallery:thumbnail", requestContext.getResolvedMount().getMount());
        assertTrue(existingImageNonPrimaryResource.isContainerResource());

        /**
         * because of HippoGalleryImageSetContainer we expact the end 'hippogallery:thumbnail' to be mapped to '/thumbnail'
          <bean class="org.hippoecm.hst.core.linking.containers.HippoGalleryImageSetContainer">
             <property name="primaryItem" value="hippogallery:original"/>
             <property name="mappings">
               <bean class="org.springframework.beans.factory.config.MapFactoryBean">
                 <property name="sourceMap">
                  <map key-type="java.lang.String" value-type="java.lang.String">
                    <entry key="hippogallery:thumbnail" value="thumbnail"/>
                  </map>
                 </property>
               </bean>
             </property>
          </bean>
         */

        assertEquals("Expected to have 'hippogallery:thumbnail' represented by '/thumbnail' right after '/binaries'",
                "binaries/thumbnail/unittestcontent/gallery/picture.jpeg", existingImageNonPrimaryResource.getPath());
        assertEquals("/site/binaries/thumbnail/unittestcontent/gallery/picture.jpeg" , existingImageNonPrimaryResource.toUrlForm(requestContext, false));
    }

    @Test
    public void binaries_link_for_non_existing_asset() throws Exception {
        HstRequestContext requestContext = initContext();
        HstLink nonExistingPdf = linkCreator.create("/binaries/unittestcontent/assets/non/existing.pdf", requestContext.getResolvedMount().getMount());
        assertTrue(nonExistingPdf.isContainerResource());
        assertEquals("binaries/unittestcontent/assets/non/existing.pdf" , nonExistingPdf.getPath());
        assertEquals("/site/binaries/unittestcontent/assets/non/existing.pdf" , nonExistingPdf.toUrlForm(requestContext, false));
    }

    @Test
    public void binaries_link_for_non_existing_gallery() throws Exception {
        HstRequestContext requestContext = initContext();
        HstLink nonExistingImage = linkCreator.create("/binaries/unittestcontent/gallery/non/existing.png", requestContext.getResolvedMount().getMount());
        assertTrue(nonExistingImage.isContainerResource());
        assertEquals("binaries/unittestcontent/gallery/non/existing.png", nonExistingImage.getPath());
        assertEquals("/site/binaries/unittestcontent/gallery/non/existing.png" , nonExistingImage.toUrlForm(requestContext, false));
    }

    @Test
    public void binaries_link_for_non_gallery_non_asset_location() throws Exception {
        HstRequestContext requestContext = initContext();
        HstLink nonExistingFoo = linkCreator.create("/binaries/unittestcontent/foo/non/existing.doc", requestContext.getResolvedMount().getMount());
        assertTrue(nonExistingFoo.isContainerResource());
        assertEquals("binaries/unittestcontent/foo/non/existing.doc", nonExistingFoo.getPath());
        assertEquals("/site/binaries/unittestcontent/foo/non/existing.doc" , nonExistingFoo.toUrlForm(requestContext, false));
    }

}
