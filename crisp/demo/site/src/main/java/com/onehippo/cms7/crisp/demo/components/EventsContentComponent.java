/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.demo.components;

import static com.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_SALES_FORCE;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.demo.beans.EventsDocument;
import com.onehippo.cms7.crisp.hst.module.CrispHstServices;

public class EventsContentComponent extends EssentialsContentComponent {

    private static Logger log = LoggerFactory.getLogger(EventsContentComponent.class);

    private static final String SOQL_ALL_LEADS = "SELECT FirstName, LastName, Status, Title, Industry, Company, NumberOfEmployees, State, Country, City, "
            + "PostalCode, Email, IsDeleted, IsConverted, ConvertedAccountId, ConvertedContactId, Rating "
            + "FROM Lead";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        EventsDocument document = (EventsDocument) request.getRequestContext().getContentBean();

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            // Note: Just as an example, let's try to find all the data by passing empty query string.
            pathVars.put("soql", SOQL_ALL_LEADS);
            Resource salesForceLeads = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_SALES_FORCE,
                    "/query/?q={soql}", pathVars);
            request.setAttribute("salesForceLeads", salesForceLeads);
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}' resource space for soql, '{}'.",
                    RESOURCE_SPACE_DEMO_SALES_FORCE, SOQL_ALL_LEADS, e);
        }
    }
}
