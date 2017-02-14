<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Video" -->
<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsVideoComponentInfo"--%> -->
<#if document??>
  <div>
    <h3>${document.title?html}</h3>
    <iframe width="${cparam.width}" height="${cparam.height}" src="${document.link?html}" frameborder="0" allowfullscreen></iframe>
    <p>${document.description?html}</p>
  </div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <div>
    <img src="<@hst.link path="/images/essentials/catalog-component-icons/video.png" />"> Click to edit Video
  </div>
</#if>
