/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.htmlcleaner;

import java.util.Map;

import javax.jcr.Node;

import junit.framework.Assert;
import nl.hippo.htmlcleaner.ElementDescriptor;
import nl.hippo.htmlcleaner.HtmlCleanerTemplate;
import nl.hippo.htmlcleaner.OutputElementDescriptor;

import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.junit.Test;

@Deprecated
public class DeprecatedHtmlCleanerConfigTest extends PluginTest {

    protected IPluginConfig getPluginConfig() throws Exception {
        Node cleanerConfigNode = root.getNode("cleaner.config");
        JcrNodeModel nodeModel = new JcrNodeModel(cleanerConfigNode);
        return new JcrPluginConfig(nodeModel);
    }

    @Test
    public void testHtmlCleanerTemplateBuilder() throws Exception {
        JCRHtmlCleanerTemplateBuilder builder = new JCRHtmlCleanerTemplateBuilder();
        
        HtmlCleanerTemplate template = builder.buildTemplate(getPluginConfig());
        
        Assert.assertEquals(JCRHtmlCleanerTemplateBuilder.SCHEMA_TRANSITIONAL,template.getXhtmlSchema());
        Assert.assertEquals(5,template.getAllowedSpanClasses().size());
        Assert.assertEquals(0,template.getAllowedDivClasses().size());
        Assert.assertEquals(3,template.getAllowedParaClasses().size());
        Assert.assertEquals(3,template.getAllowedPreClasses().size());
        
        Assert.assertNull(template.getImgAlternateSrcAttr());
        
        Map descriptors = template.getDescriptors();
        Assert.assertEquals(32,descriptors.size());

        ElementDescriptor span = (ElementDescriptor) descriptors.get("span");
        Assert.assertNotNull(span);
        Assert.assertEquals(2,span.getAttributeNames().length);
        
        Map outputElements = template.getOutputElementDescriptors();
        Assert.assertNotNull(outputElements);
        
        Assert.assertEquals(17, outputElements.size());
        
        OutputElementDescriptor html = (OutputElementDescriptor) outputElements.get("html");
        Assert.assertNotNull(html);
        Assert.assertEquals(1, html.getNewLinesAfterOpenTag());
        Assert.assertEquals(1, html.getNewLinesBeforeCloseTag());
        Assert.assertEquals(0, html.getNewLinesAfterCloseTag());
        Assert.assertEquals(0, html.getNewLinesBeforeOpenTag());
        Assert.assertEquals(JCRHtmlCleanerTemplateBuilder.DEFAULT_INLINE, html.isInline());
        
        Assert.assertEquals(80,template.getMaxLineWidth());
    }
}
