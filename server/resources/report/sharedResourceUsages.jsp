

<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<%--@elvariable id="totalUsagesNum" type="java.lang.Integer"--%>
<%--@elvariable id="resource" type="jetbrains.buildServer.sharedResources.model.resources.Resource"--%>
<%--@elvariable id="buildTypes" type="java.util.Map<jetbrains.buildServer.serverSide.SBuildType, java.util.List<jetbrains.buildServer.sharedResources.model.Lock>>"--%>
<%--@elvariable id="templates" type="java.util.Map<jetbrains.buildServer.serverSide.BuildTypeTemplate, java.util.List<jetbrains.buildServer.sharedResources.model.Lock>>"--%>

<c:if test="${not empty resource}">

  <h2 class="noBorder"><c:out value="Shared resource: ${resource.name}"/></h2>

  <c:if test="${totalUsagesNum eq 0}">
    <div class="usagesSection">This resource is unused.</div>
  </c:if>

  <c:if test="${not empty templates}">
    <div class="usagesSection">
      <div>
        Used in <strong>${fn:length(templates)}</strong> template<bs:s val="${fn:length(templates)}"/>:
      </div>
      <ul>
        <c:forEach items="${templates}" var="entry">
          <c:set var="btSettings" value="${entry.key}"/>
          <li>
            <c:set var="canEdit" value="${afn:permissionGrantedForProject(btSettings.project, 'EDIT_PROJECT')}"/>
            <c:choose>
              <c:when test="${canEdit}">
                <admin:editTemplateLink step="buildFeatures" templateId="${btSettings.externalId}"><c:out value="${btSettings.fullName}"/></admin:editTemplateLink>
              </c:when>
              <c:otherwise><c:out value="${btSettings.fullName}"/></c:otherwise>
            </c:choose>
          </li>
        </c:forEach>
      </ul>
    </div>
  </c:if>

  <c:if test="${not empty buildTypes}">
    <div class="usagesSection">
      <div>
        Used in <strong>${fn:length(buildTypes)}</strong> build configuration<bs:s val="${fn:length(buildTypes)}"/>:
      </div>
      <ul>
        <c:forEach items="${buildTypes}" var="entry">
          <c:set var="btSettings" value="${entry.key}"/>
          <li>
            <c:set var="canEdit" value="${afn:permissionGrantedForBuildType(btSettings, 'EDIT_PROJECT') and (not btSettings.templateBased or btSettings.templateAccessible)}"/>
            <c:choose>
              <c:when test="${canEdit}">
                <admin:editBuildTypeLinkFull step="buildFeatures" buildType="${btSettings}"/>
              </c:when>
              <c:otherwise><c:out value="${btSettings.fullName}"/></c:otherwise>
            </c:choose>
          </li>
        </c:forEach>
      </ul>
    </div>
  </c:if>
</c:if>