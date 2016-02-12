<#include "../include/imports.ftl">

<#-- @ftlvariable name="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu" -->
<#if menu??>
<ul class="nav nav-pills">
    <#list menu.siteMenuItems as item>
        <#if !item.hstLink?? && !item.externalLink??>
            <#if item.selected || item.expanded>
              <li class="active"><span>${item.name?html}</span></li>
            <#else>
              <li><span>${item.name?html}</span></li>
            </#if>
        <#else>
            <#if item.hstLink??>
                <#assign href><@hst.link link=item.hstLink/></#assign>
            <#elseif item.externalLink??>
                <#assign href>${item.externalLink?replace("\"", "")}</#assign>
            </#if>
            <#if  item.selected || item.expanded>
              <li class="active"><a href="${href}">${item.name?html}</a></li>
            <#else>
              <li><a href="${href}">${item.name?html}</a></li>
            </#if>
        </#if>
    </#list>
</ul>
    <@hst.cmseditmenu menu=menu/>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<img src="<@hst.link path="/images/essentials/catalog-component-icons/menu.png" />"> Click to edit Menu
</#if>