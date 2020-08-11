<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="com.bloomreach.beans.Banner" -->
<#if document??>
<div>
  <#if hst.isNodeType(document.node, 'xmspaexample:bannerdocument')>
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
  <#elseif editMode>
    <figure style="position: relative">
      <@hst.manageContent documentTemplateQuery="new-banner-document" parameterName="document" rootPath="banners"/>
      <img src="<@hst.link path='/images/essentials/catalog-component-icons/banner.svg'/>"> Selected document "${document.node.path}" is not of the correct type, please select or create a Banner document.
    </figure>
  </#if>
</div>
<#elseif editMode>
<div>
  <figure style="position: relative">
    <@hst.manageContent documentTemplateQuery="new-banner-document" parameterName="document" rootPath="banners"/>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/banner.svg'/>"> Click to edit Banner
  </figure>
</div>
</#if>
