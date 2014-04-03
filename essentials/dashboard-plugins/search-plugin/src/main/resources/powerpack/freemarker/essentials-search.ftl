<#include "/WEB-INF/ftl/essentials/common/imports.ftl">
<#-- @ftlvariable name="query" type="java.lang.String" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="showPagination" type="java.lang.Boolean" -->
<#-- @ftlvariable name="item" type="{{beansPackage}}.NewsDocument" -->
<form class="navbar-form" role="search" action="<@hst.link path="/search" />" method="get">
    <div class="input-group">
        <input type="text" class="form-control" placeholder="Search" name="query">
        <div class="input-group-btn">
            <button class="btn btn-default" type="submit"><i class="glyphicon glyphicon-search"></i></button>
        </div>
    </div>
</form>
<#if pageable??>
    <#if pageable?? && pageable.total ==0>
    <h3>No results for: ${query}</h3>
    <#else>
        <#list pageable.items as item>
            <@hst.link var="link" hippobean=item />
        <article>
            <h3><a href="${link}">${item.title}</a></h3>
        </article>
        </#list>
        <#if showPagination??>
            <#include "/WEB-INF/ftl/essentials/common/pagination.ftl">
        </#if>
    </#if>
</#if>