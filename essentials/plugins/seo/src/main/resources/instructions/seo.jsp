<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<c:if test="${not empty title}">
  <hst:headContribution category="SEO" keyHint="hst.seo.document.title">
    <title><c:out value="${title}"/></title>
  </hst:headContribution>
</c:if>

<c:if test="${not empty metaKeywords}">
  <hst:headContribution category="SEO" keyHint="hst.seo.document.keywords">
    <meta name="keywords" content="${fn:escapeXml(metaKeywords)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty metaDescription}">
  <hst:headContribution category="SEO" keyHint="hst.seo.document.description">
    <meta name="description" content="${fn:escapeXml(metaDescription)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty dublinCoreSchemaLink}">
  <hst:headContribution category="SEO" keyHint="hst.seo.schema.dc">
    <link rel="schema.DC" href="${fn:escapeXml(dublinCoreSchemaLink)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty dublinCoreTermsLink}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.terms">
    <link rel="schema.DCTERMS" href="${fn:escapeXml(dublinCoreTermsLink)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty dublinCoreCopyrightLink}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.copyright">
    <link rel="DC.rights copyright" href="${fn:escapeXml(dublinCoreCopyrightLink)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty dublinCoreLanguage}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.language">
    <meta scheme="DCTERMS.RFC3066" name="DC.language" content="${fn:escapeXml(dublinCoreLanguage)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty dublinCoreTermsCreated}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.terms.created">
    <meta name="DCTERMS.created" content="${fn:escapeXml(dublinCoreTermsCreated)}"/>
  </hst:headContribution>
</c:if>

<c:if test="${not empty dublinCoreTermsModified}">
  <hst:headContribution category="SEO" keyHint="hst.seo.dc.terms.modified">
    <meta name="DCTERMS.modified" content="${fn:escapeXml(dublinCoreTermsModified)}"/>
  </hst:headContribution>
</c:if>


<hst:defineObjects/>
<c:if test="${hstRequest.requestContext.cmsRequest}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/seo.png'/>"> Click to edit SEO parameters
</c:if>
