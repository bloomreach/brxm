<#include "/WEB-INF/freemarker/include/imports.ftl">
<#-- @ftlvariable name="item" type="{{beansPackage}}.Blogpost" -->
<#-- @ftlvariable name="author" type="{{beansPackage}}.Author" -->
<#-- @ftlvariable name="showPagination" type="java.lang.Boolean" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->

<div class="panel panel-default">
<#if pageable??>
    <div class="panel-heading">
        <h3 class="panel-title">More by ${author.fullName}</h3>
    </div>
    <#if pagebale?? && pageable.total > 0>
        <div class="panel-body">
            <#list pageable.items as item>
                <@hst.link hippobean=item var="link"/>
                <p><a href="${link}">${item.title}</a></p>
            </#list>
        </div>
    <#else>
        <div class="panel-body">
            <p>No other posts found.</p>
        </div>
    </#if>
</#if>
</div>