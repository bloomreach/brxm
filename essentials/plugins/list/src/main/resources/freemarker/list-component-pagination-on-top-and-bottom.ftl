<#include "../../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#if pageable?? && pageable.items?has_content>
  <div>
    <#if cparam.showPagination>
        <#include "../../include/pagination.ftl">
    </#if>
    <#list pageable.items as item>
        <#if item.title??>
            <#assign linkName=item.title>
        <#else>
            <#assign linkName=item.localizedName>
        </#if>

      <article class="has-edit-button">
        <@hst.manageContent hippobean=item />
        <@hst.link var="link" hippobean=item />
        <h3><a href="${link}">${linkName?html}</a></h3>
        <#if item.introduction??>
          <p>${item.introduction?html}</p>
        </#if>
      </article>
    </#list>
    <div class="has-new-content-button">
      <@hst.manageContent documentTemplateQuery="new-document"/>
    </div>
    <#if cparam.showPagination>
        <#include "../../include/pagination.ftl">
    </#if>
  </div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
  <div>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/generic-list.svg'/>"> Click to edit Generic List
  </div>
</#if>