<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Video" -->
<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsVideoComponentInfo"--%> -->
<#if document??>
  <h3>${document.title}</h3>
  <iframe width="${cparam.width}" height="${cparam.height}" src="${document.link}" frameborder="0" allowfullscreen></iframe>
  <p>${document.description}</p>
<#-- @ftlvariable id="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<img src="<@hst.link path="/images/essentials/catalog-component-icons/video.png" />"> Click to edit Video
</#if>
