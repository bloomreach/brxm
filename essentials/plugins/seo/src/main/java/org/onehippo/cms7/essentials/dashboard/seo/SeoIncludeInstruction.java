/*
 *   Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.onehippo.cms7.essentials.dashboard.seo;

import java.io.File;
import java.util.Map;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.MarkupCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeoIncludeInstruction implements Instruction {

    private static final Logger log = LoggerFactory.getLogger(SeoIncludeInstruction.class);

    public static final String SEO_HST_INCLUDE = "seohelper";

    @Override
    public InstructionStatus process(final PluginContext context, final InstructionStatus previousStatus) {
        processPlaceholders(context.getPlaceholderData());
        final String templateLanguage = context.getProjectSettings().getTemplateLanguage();
        final String namespace = (String)context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_NAMESPACE);
        final String templatePath;
        final MarkupCodeUtils.TemplateType type;
        if (templateLanguage.equals("jsp")) {
            type = MarkupCodeUtils.TemplateType.JSP;
            templatePath = (String)context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_JSP_ROOT) + '/' + namespace + '/' + "base-layout.jsp";
        } else {
            type = MarkupCodeUtils.TemplateType.FREEMARKER;
            templatePath = (String)context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_WEBFILES_FREEMARKER_ROOT) + '/' + namespace + '/' + "base-layout.ftl";
        }
        log.debug("Injecting SEO include {}", templatePath);

        final boolean included = MarkupCodeUtils.addHstIncludeAsFirstBody(new File(templatePath), SEO_HST_INCLUDE, type);
        if (!included) {
            return InstructionStatus.FAILED;
        }
        return InstructionStatus.SUCCESS;
    }


    @Override
    public void processPlaceholders(final Map<String, Object> data) {

    }

    @Override
    public String getMessage() {
        return "SEO HST include";
    }

    @Override
    public void setMessage(final String message) {

    }

    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setAction(final String action) {

    }

}
