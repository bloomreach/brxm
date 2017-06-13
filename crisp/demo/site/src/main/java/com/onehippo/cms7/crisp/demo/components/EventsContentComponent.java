/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.demo.components;

import static com.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_MARKETO;
import static com.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_SALES_FORCE;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.essentials.components.EssentialsContentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.hst.module.CrispHstServices;

public class EventsContentComponent extends EssentialsContentComponent {

    private static Logger log = LoggerFactory.getLogger(EventsContentComponent.class);

    private static final String SOQL_ALL_LEADS = "SELECT FirstName, LastName, Status, Title, Industry, Company, NumberOfEmployees, State, Country, City, "
            + "PostalCode, Email, IsDeleted, IsConverted, ConvertedAccountId, ConvertedContactId, Rating "
            + "FROM Lead";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        log.warn("\n\n===============================================================================\n"
                + "  [INFO] SALESFORCE LEADS DEMO\n"
                + "===============================================================================\n\n");
        Resource salesForceLeads = findSalesForceLeads();

        if (salesForceLeads != null) {
            request.setAttribute("salesForceLeads", salesForceLeads);
        }

        log.warn("\n\n===============================================================================\n"
                + "  [INFO] MARKETO LEAD CHANGES DEMO\n"
                + "===============================================================================\n\n");
        Resource marketoLeadChanges = findMarketoLeads();

        if (marketoLeadChanges != null) {
            request.setAttribute("marketoLeadChanges", marketoLeadChanges);
        }
    }

    /**
     * Simple example to retrieve SalesForce lead data using SOQL.
     * @return SalesForce lead data resource
     */
    private Resource findSalesForceLeads() {
        Resource salesForceLeads = null;

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
            final Map<String, Object> pathVars = new HashMap<>();
            // Note: Just as an example, let's try to find all the data by passing empty query string.
            pathVars.put("soql", SOQL_ALL_LEADS);
            salesForceLeads = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_SALES_FORCE,
                    "/query/?q={soql}", pathVars);
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}' resource space for soql, '{}'.",
                    RESOURCE_SPACE_DEMO_SALES_FORCE, SOQL_ALL_LEADS, e);
        }

        return salesForceLeads;
    }

    /**
     * Simple example to retrieve lead change data during last 1 week using Marketo REST API:
     * <UL>
     * <LI><A href="http://developers.marketo.com/rest-api/endpoint-reference/lead-database-endpoint-reference/#!/Activities/getActivitiesPagingTokenUsingGET">/rest/v1/activities/pagingtoken.json</A></LI>
     * <LI><A href="http://developers.marketo.com/rest-api/endpoint-reference/lead-database-endpoint-reference/#!/Activities/getLeadChangesUsingGET">/rest/v1/activities/leadchanges.json</A></LI>
     * </UL>
     * @return Marketo lead changes resource object
     */
    private Resource findMarketoLeads() {
        Resource marketoLeadChanges = null;

        try {
            ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();

            Calendar weekAgo = Calendar.getInstance();
            weekAgo.add(Calendar.DATE, -7);
            Resource nextPagingRes = getMarketoNextPaging(resourceServiceBroker, weekAgo);

            final Map<String, Object> pathVars = new HashMap<>();
            pathVars.put("nextPageToken", nextPagingRes.getValueMap().get("nextPageToken"));
            pathVars.put("fields", "email,firstName,lastName");
            marketoLeadChanges = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_MARKETO,
                    "/activities/leadchanges.json?nextPageToken={nextPageToken}&fields={fields}", pathVars);
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}' resource space for path, '{}'.", RESOURCE_SPACE_DEMO_MARKETO,
                    "/activities/leadchanges.json?nextPageToken={nextPageToken}&fields={fields}", e);
        }

        return marketoLeadChanges;
    }

    private Resource getMarketoNextPaging(final ResourceServiceBroker resourceServiceBroker, final Calendar sinceDate) {
        Resource marketoNextPaging = null;

        try {
            final Map<String, Object> pathVars = new HashMap<>();
            pathVars.put("sinceDatetime", ISO8601.format(sinceDate));
            marketoNextPaging = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_MARKETO,
                    "/activities/pagingtoken.json?sinceDatetime={sinceDatetime}", pathVars);
        } catch (Exception e) {
            log.warn("Failed to find resources from '{}' resource space for path, '{}'.", RESOURCE_SPACE_DEMO_MARKETO,
                    "/activities/pagingtoken.json?sinceDatetime={sinceDatetime}", e);
        }

        return marketoNextPaging;
    }
}
