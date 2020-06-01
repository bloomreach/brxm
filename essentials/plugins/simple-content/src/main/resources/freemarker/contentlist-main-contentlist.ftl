<#include "../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="item" type="{{beansPackage}}.ContentDocument" -->
<#if pageable?? && pageable.items?has_content>
<div>
  <#list pageable.items as item>
    <article class="has-edit-button">
      <@hst.manageContent hippobean=item/>
      <@hst.link var="link" hippobean=item/>
      <h3><a href="${link}">${item.title?html}</a></h3>
      <#if item.publicationDate??>
        <p>
          <@fmt.formatDate value=item.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
        </p>
      </#if>
      <#if item.introduction??>
        <p>${item.introduction?html}</p>
      </#if>
    </article>
  </#list>
  <div class="has-new-content-button">
    <@hst.manageContent documentTemplateQuery="new-content-document" rootPath="content"/>
  </div>
  <#if cparam.showPagination>
    <#include "../include/pagination.ftl">
  </#if>
</div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <div>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/generic-list.png'/>"> Click to edit Content list
    <div class="has-new-content-button">
      <@hst.manageContent documentTemplateQuery="new-content-document" rootPath="content"/>
    </div>
  </div>
</#if>
