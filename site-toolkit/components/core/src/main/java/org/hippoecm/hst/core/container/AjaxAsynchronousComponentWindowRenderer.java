/**
 * Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * AjaxAsynchronousComponentWindowRenderer
 * <P>
 * Asynchronous component window rendering implementation leveraging AJAX technologies.
 * </P>
 */
public class AjaxAsynchronousComponentWindowRenderer extends AbstractAsynchronousComponentWindowRenderer {

    private static Logger log = LoggerFactory.getLogger(AjaxAsynchronousComponentWindowRenderer.class);

    /**
     * Some utility methods to get obfuscated inline javascript for loading async pages
     */
    private static final char RANDOM_CHAR1 = (char)('a' + new Random().nextInt(10));
    private static final char RANDOM_CHAR2 =  (char) (RANDOM_CHAR1 + 1);
    private static final char RANDOM_CHAR3 =  (char) (RANDOM_CHAR1 + 2);
    private static final String OBFUSCATED_ASYNC_VAR = String.valueOf(RANDOM_CHAR3) + AjaxAsynchronousComponentWindowRenderer.class.hashCode() + "Async";
    private static final String OBFUSCATED_HIPPO_VAR = String.valueOf(RANDOM_CHAR1) + AjaxAsynchronousComponentWindowRenderer.class.hashCode();
    private static final String OBFUSCATED_HIPPO_HST_VAR = OBFUSCATED_HIPPO_VAR + "." + String.valueOf(RANDOM_CHAR2) + AjaxAsynchronousComponentWindowRenderer.class.hashCode();
    private static final String HEAD_SCRIPT_KEY_HINT = AjaxAsynchronousComponentWindowRenderer.class.getName() + ".async";
    private static final String SIMPLE_IO_SCRIPT_RESOURCE_PATH = "simple-io-template.js";

    // no need to be volatile : worst case it is created twice
    private static String obfuscatedScript = null;

    private String asyncLoadJavaScriptFragmentTemplate;

    public String getAsyncLoadJavaScriptFragmentTemplate() {
        return asyncLoadJavaScriptFragmentTemplate;
    }

    public void setAsyncLoadJavaScriptFragmentTemplate(String asyncLoadJavaScriptFragmentTemplate) {
        this.asyncLoadJavaScriptFragmentTemplate = asyncLoadJavaScriptFragmentTemplate;
    }

    @Override
    public void processWindowBeforeRender(HstComponentWindow window, HstRequest request, HstResponse response) {
        HstURL url = createAsyncComponentRenderingURL(request, response);
        Element hiddenDiv = response.createElement("div");
        hiddenDiv.setAttribute("id", url.toString());
        hiddenDiv.setAttribute("class", OBFUSCATED_ASYNC_VAR);
        hiddenDiv.setAttribute("style", "display:none;");
        response.addPreamble(hiddenDiv);

        if (!response.containsHeadElement(HEAD_SCRIPT_KEY_HINT)) {
            Element headScript = response.createElement("script");
            headScript.setAttribute("type","text/javascript");
            headScript.setTextContent(getIOScript());
            response.addHeadElement(headScript, HEAD_SCRIPT_KEY_HINT);

            Element endBodyScript = response.createElement("script");
            endBodyScript.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "scripts");
            endBodyScript.setAttribute("type", "text/javascript");

            String asyncLoadJavaScriptFragment = OBFUSCATED_HIPPO_HST_VAR + ".AsyncPage.load();";
            if (StringUtils.isNotBlank(asyncLoadJavaScriptFragmentTemplate)) {
                try {
                    asyncLoadJavaScriptFragment = MessageFormat.format(asyncLoadJavaScriptFragmentTemplate,
                            asyncLoadJavaScriptFragment);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid message format for 'ajax.asynchronous.component.windows.load.js.fragment.template'"
                            + "property in the configuration: '{}'. Did you follow java.text.MessageFormat format?",
                            asyncLoadJavaScriptFragmentTemplate);
                }
            }
            endBodyScript.setTextContent(asyncLoadJavaScriptFragment);
            response.addHeadElement(endBodyScript, "asyncLoad");
        }
    }

    private static String obfuscateNamespacedFunctions(final String ioScriptTemplate) {
        log.debug("creating obfuscated io-script with RANDOM CHAR", OBFUSCATED_HIPPO_HST_VAR);
        String obfuscated = ioScriptTemplate.replaceAll("Hippo.Hst", OBFUSCATED_HIPPO_HST_VAR);
        obfuscated = obfuscated.replaceAll("Hippo", OBFUSCATED_HIPPO_VAR);
        obfuscated = obfuscated.replaceAll("_async", OBFUSCATED_ASYNC_VAR);
        return obfuscated;
    }

    private static String getIOScript() {
        if (obfuscatedScript != null) {
            return obfuscatedScript;
        }
        final String ioScriptTemplate = loadScript();
        obfuscatedScript =  obfuscateNamespacedFunctions(ioScriptTemplate);
        return obfuscatedScript; 
    }

    private static String loadScript() {
        InputStream input = null;
        try {
            input = AjaxAsynchronousComponentWindowRenderer.class.getResourceAsStream(SIMPLE_IO_SCRIPT_RESOURCE_PATH);
            if (input == null) {
                log.warn("Could not load simple-io-template.js");
                return "";
            }
            String ioScriptTemplate = IOUtils.toString(input, "UTF-8");
            return ioScriptTemplate;
        } catch (IOException e) {
            log.warn("Could not load simple-io-template.js");
        } finally {
            IOUtils.closeQuietly(input);
        }
        return "";
    }
}
