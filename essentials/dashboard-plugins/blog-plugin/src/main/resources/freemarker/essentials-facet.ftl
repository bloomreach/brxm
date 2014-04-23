<#include "/WEB-INF/freemarker/include/imports.ftl">
<#--
  Copyright 2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#-- @ftlvariable name="facets" type="org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean" -->
<#-- @ftlvariable name="facetLimit" type="java.lang.Integer" -->
<#-- @ftlvariable name="query" type="java.lang.String" -->
<#if facets??>
    <#assign facetLimit = 50>

<form action="<@hst.actionURL />" method="get">
    <div class="row form-group">
        <div class="col-xs-8">
            <#if query??>
                <input type="search" value="${query}" name="query" class="form-control" placeholder="Search blog posts">
            <#else>
                <input type="search" value="" name="query" class="form-control" placeholder="Search blog posts">
            </#if>

        </div>
        <div class="col-xs-4">
            <button type="submit" class="btn btn-primary pull-right">Search</button>
        </div>
    </div>
</form>
<ul class="nav nav-list">
    <#list  facets.folders as facetvalue>
        <#if facetvalue.folders??>
            <li><label class="nav-header">${facetvalue.name}</label>
                <ul class="nav nav-list">
                    <#list  facetvalue.folders as item>
                        <#if (item_index <= facetLimit)>
                            <#if (item.leaf?? && item.leaf && (item.count > 0))>
                                <@hst.facetnavigationlink  current=facets remove=item var="removeLink"/>
                                <li class="active">
                                    <a href="${removeLink}">${item.name}&nbsp;<span class="alert-danger">remove</span></a>
                                </li>
                            <#else>
                                <@hst.link var="link" hippobean=item navigationStateful=true/>
                                <li><a href="${link}">${item.name}&nbsp;<span>(${item.count})</span></a></li>
                            </#if>
                        </#if>
                        <#if (item_index > facetLimit)>
                            <#if item.leaf && item.count > 0>
                                <@hst.facetnavigationlink remove=item current=facets var="removeLink"/>
                                <li class="active"><a href="${removeLink}"><${item.name}</a></li>

                            <#else>
                                <@hst.link var="link" hippobean=item navigationStateful=true/>
                                <li class="extra">
                                    <a href="${link}">${item.name}&nbsp;<span>(${item.count})</span></a>
                                </li>
                            </#if>
                        </#if>
                    </#list>
                </ul>
            </li>
        </#if>
    </#list>
</ul>
</#if>