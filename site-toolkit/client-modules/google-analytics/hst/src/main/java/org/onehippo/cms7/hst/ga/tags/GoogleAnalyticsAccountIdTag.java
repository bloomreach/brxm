/*
 *  Copyright 2011-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.hst.ga.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.googleanalytics.GoogleAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAnalyticsAccountIdTag extends TagSupport {

    private static final Logger log = LoggerFactory.getLogger(GoogleAnalyticsAccountIdTag.class);

    private static final long serialVersionUID = 1L;

    private static final String GA_ACCOUNT_ID_SCRIPT_TEMPLATE =
            "<script type=\"text/javascript\">\n" +
            "  Hippo_Ga_AccountId='%s';\n" +
            "</script>\n";

    /**
     * Custom Google Analytics Account ID value.
     */
    private String value;

    /**
     * Return custom Google Analytics Account ID value.
     * @return custom Google Analytics Account ID value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set custom Google Analytics Account ID value.
     * @param value custom Google Analytics Account ID value
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            String accountId = getValue();

            if (accountId == null || "".equals(accountId)) {
                GoogleAnalyticsService service = HippoServiceRegistry.getService(GoogleAnalyticsService.class);

                if (service != null) {
                    accountId = service.getAccountId();
                }
            }

			if (accountId != null && !"".equals(accountId)) {
				pageContext.getOut().write(String.format(GA_ACCOUNT_ID_SCRIPT_TEMPLATE, accountId));
			} else {
				log.warn("Google Analytics Account ID is not set!");
			}
        }
        catch (IOException e) {
            throw new JspException("IOException while trying to write script tag", e);
        }

        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        value = null;

        return EVAL_PAGE;
    }

}
