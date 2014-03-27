<%@ include file="/WEB-INF/jsp/essentials/common/imports.jsp" %>
<#-- @ftlvariable name="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu" -->
<#if menu??>
<ul class="nav nav-pills">
    <#list menu.siteMenuItems as item>
        <#if  item.selected || item.expanded>
            <li class="active"><a href="<@hst.link link=item.hstLink/>">${item.name}</a></li>
        <#else>
            <li><a href="<@hst.link link=item.hstLink/>">${item.name}</a></li>
        </#if>
    </#list>
</ul>
</#if>