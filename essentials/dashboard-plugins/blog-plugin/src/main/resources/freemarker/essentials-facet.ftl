<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="facets" type="org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean" -->
<#-- @ftlvariable name="facetLimit" type="java.lang.Integer" -->
<#-- @ftlvariable name="query" type="java.lang.String" -->
<#if facets??>
    <@c.set var="facetLimit" value=50/>

<form action="<@hst.actionURL />" method="get">
    <div class="form-group">
        <div class="col-xs-8">
            <input type="search" value="${query}" name="query" class="form-control" placeholder="Search blog posts">
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

                        <#if index.count <= facetLimit>
                            <#if item.leaf?? && item.count > 0>
                                <@hst.facetnavigationlink remove="${item}" current="${facets}" var="removeLink"/>
                                <li class="active">
                                    <a href="${removeLink}">${item.name}&nbsp;<span class="alert-danger">remove</span></a>
                                </li>
                            <#else>
                                <@hst.link var="link" hippobean=item navigationStateful=true/>
                                <li><a href="${link}">${item.name}&nbsp;<span>(${item.count})</span></a></li>
                            </#if>
                        </#if>
                        <#if index.count > facetLimit>
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



