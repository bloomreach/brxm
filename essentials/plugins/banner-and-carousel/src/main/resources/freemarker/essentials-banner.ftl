<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.Banner" -->
<#if document??>
<div class="row">
    <a href="<@hst.link hippobean=document.link />"><img src="<@hst.link hippobean=document.image />" alt="${document.title}"/></a>
</div>
<#elseif editMode>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/banner.png'/>"> Click to edit Banner
</#if>
