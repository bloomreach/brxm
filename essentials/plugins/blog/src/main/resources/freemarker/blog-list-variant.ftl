<#include "../../../include/imports.ftl">

<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<@hst.setBundle basename="essentials.blog"/>
<#if pageable?? && pageable?has_content>
<div>
  <#list pageable.items as item>
    <div class="media has-edit-button">
      <@hst.manageContent hippobean=item/>
      <div class="media-body">
        <h4 class="media-heading">
          <@hst.link var="link" hippobean=item />
          <a href="${link}">${item.title?html}</a>
          <#if item.publicationDate??>
            <span class="label label-success pull-right">
              <@fmt.formatDate value=item.publicationDate.time type="both" dateStyle="medium" timeStyle="short"/>
            </span>
          </#if>
        </h4>
        <#if item.introduction??>
          <p>${item.introduction?html}</p>
        </#if>
        <p><a href="${link}"><@fmt.message key="blog.read.post" var="msg"/>${msg?html}</a></p>
      </div>
    </div>
  </#list>
  <div class="has-new-content-button">
    <@hst.manageContent documentTemplateQuery="new-blog-document" folderTemplateQuery="new-blog-folder" rootPath="blog" defaultPath="${currentYear}/${currentMonth}"/>
  </div>
  <#if cparam.showPagination>
    <#include "../../../include/pagination.ftl">
  </#if>
</div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/blog-list.svg'/>"> Click to edit Blog List
</div>
</#if>