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