/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.scxml2.io.SCXMLReader.Configuration;
import org.apache.commons.scxml2.model.CustomAction;
import org.apache.commons.scxml2.model.SCXML;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.action.ArchiveDelegatingAction;
import org.onehippo.repository.documentworkflow.action.CopyDocumentDelegatingAction;
import org.onehippo.repository.documentworkflow.action.CopyVariantDelegatingAction;
import org.onehippo.repository.documentworkflow.action.IsModifiedDelegatingAction;
import org.onehippo.repository.documentworkflow.action.MoveDocumentDelegatingAction;
import org.onehippo.repository.documentworkflow.action.RenameDocumentDelegatingAction;
import org.onehippo.repository.documentworkflow.action.RequestDelegatingAction;
import org.onehippo.repository.documentworkflow.action.ScheduleRequestDelegatingAction;
import org.onehippo.repository.scxml.SCXMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * InRepoFullWorkflowParallelModelTransitionTest
 */
public class InRepoFullWorkflowParallelModelTransitionTest {

    private static Logger log = LoggerFactory.getLogger(InRepoFullWorkflowParallelModelTransitionTest.class);

    private SCXML scxml;

    protected List<CustomAction> getCustomActions() {
        List<CustomAction> customActions = new LinkedList<>();

        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "copyvariant", CopyVariantDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "request", RequestDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "archive", ArchiveDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "ismodified", IsModifiedDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "schedulerequest", ScheduleRequestDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "copydocument", CopyDocumentDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "movedocument", MoveDocumentDelegatingAction.class));
        customActions.add(new CustomAction("http://www.onehippo.org/cms7/repository/scxml", "renamedocument", RenameDocumentDelegatingAction.class));

        return customActions;
    }

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration(null, null, getCustomActions());

        InputStream is = null;

        try {
            is = getClass().getResourceAsStream("/scxml-definitions.xml");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            XPathExpression xpathExpr = XPathFactory.newInstance().newXPath().compile("//node[@name='reviewed-actions-workflow']/property[@name='hipposcxml:source']/value/text()");
            String scxmlSource = StringUtils.trim((String) xpathExpr.evaluate(doc.getDocumentElement(), XPathConstants.STRING));
            //log.debug("scxml source:\n{}", scxmlSource);
            scxml = SCXMLUtils.loadSCXML(IOUtils.toInputStream(scxmlSource.toString()), configuration);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test
    public void testInit() throws Exception {
    }

}
