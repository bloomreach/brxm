<#include "../../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Banner" -->
<#if document??>
  <div>
    <a href="<@hst.link hippobean=document.link />">
      <figure style="position: relative">
        <@hst.manageContent document=document/>
        <img src="<@hst.link hippobean=document.image />" alt="${document.title?html}"/>
      </figure>
    </a>
  </div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <figure style="position: relative">
    <@hst.manageContent templateQuery="new-banner-document" componentParameter="document" rootPath="banners"/>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
  </figure>
</div>
</#if>
