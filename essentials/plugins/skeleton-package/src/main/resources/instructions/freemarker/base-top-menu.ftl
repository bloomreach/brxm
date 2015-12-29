<#include "../include/imports.ftl">

<#-- @ftlvariable name="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu" -->
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#if menu??>
  <#if menu.siteMenuItems??>
    <ul class="nav nav-pills">
      <#list menu.siteMenuItems as item>
        <#if item.hstLink??>
          <#assign href><@hst.link link=item.hstLink/></#assign>
        <#elseif item.externalLink??>
          <#assign href>${item.externalLink}</#assign>
        <#else>
          <#assign href=''>
        </#if>
        <#if item.selected || item.expanded>
          <li class="active"><a href="${href}">${item.name?html}</a></li>
        <#else>
          <li><a href="${href}">${item.name?html}</a></li>
        </#if>
      </#list>
    </ul>
  </#if>
  <@hst.cmseditmenu menu=menu/>
<#else>
  <#if editMode>
    <h5>[Menu Component]</h5>
    <sub>Click to edit Menu</sub>
  </#if>
</#if>