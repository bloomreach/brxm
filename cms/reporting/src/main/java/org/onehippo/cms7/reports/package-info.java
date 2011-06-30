/**
 * Copyright (c) 2011 Hippo B.V.
 */

/**
 * This package provides CMS7 Reporting perspective and plugins.
 * The main class in this package is {@link ReportsPerspective}, which loads the perspective.
 * All the plugins in this perspective are currently loaded via configuration specified in <i>hippo-reports.xml</i>
 * Each of the plugin that needs to be added in this perspective should use the <i>wicket:id - service.reports.report</i>
 * in its configuration.
 *
 * This package also provides the {@link ExtPlugin} which should be extended by all the plugins which need to be added
 * to the ReportsPerspective.
 */
package org.onehippo.cms7.reports;