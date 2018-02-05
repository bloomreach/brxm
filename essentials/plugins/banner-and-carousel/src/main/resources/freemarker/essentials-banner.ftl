<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Banner" -->
<#if document??>
<div>
  <a href="<@hst.link hippobean=document.link />">
    <figure style="position: relative">
      <@hst.manageContent hippobean=document parameterName="document" rootPath="banners"/>
      <img src="<@hst.link hippobean=document.image />" alt="${document.title?html}"/>
      <figcaption style="position:absolute; top:20px; left:20px; z-index:100; color:white; background: rgba(51, 122, 183, 0.7); width:60%; padding:0 20px 20px 20px; text-shadow: 0 1px 2px rgba(0, 0, 0, .6);">
        <#if document.title??>
          <h3>${document.title?html}</h3>
        </#if>
        <@hst.html hippohtml=document.content/>
      </figcaption>
    </figure>
  </a>
</div>
<#elseif editMode>
<div>
  <figure style="position: relative">
    <@hst.manageContent templateQuery="new-banner-document" parameterName="document" rootPath="banners"/>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
  </figure>
</div>
</#if>
