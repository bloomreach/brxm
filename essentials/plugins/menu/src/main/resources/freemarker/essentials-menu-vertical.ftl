<#include "../../include/imports.ftl">

<#-- @ftlvariable name="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu" -->
<#if menu??>
<ul class="nav nav-pills nav-stacked">
    <#list menu.siteMenuItems as item>
        <#if  item.selected || item.expanded>
            <li class="active"><a href="<@hst.link link=item.hstLink/>">${item.name?html}</a></li>
        <#else>
            <li><a href="<@hst.link link=item.hstLink/>">${item.name?html}</a></li>
        </#if>
    </#list>
</ul>
    <@hst.cmseditmenu menu=menu/>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<img src="<@hst.link path="/images/essentials/catalog-component-icons/menu.png" />"> Click to edit Menu
</#if>