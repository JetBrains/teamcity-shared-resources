
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/include-internal.jsp" %>
<%@ page import="jetbrains.buildServer.sharedResources.model.Lock" %>
<%@ page import="jetbrains.buildServer.sharedResources.pages.ResourceHelper" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<jsp:useBean id="showMode" type="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" scope="request"/>
<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>

<c:set var="invalidLocks" value="${healthStatusItem.additionalData['invalid_locks']}"/>
<c:set var="buildType" value="${healthStatusItem.additionalData['build_type']}"/>

<c:if test="${not empty invalidLocks}">
  <div>
    <bs:buildTypeLink buildType="${buildType}">
      <bs:out value="${buildType.extendedFullName}"/>
    </bs:buildTypeLink> contains invalid lock<bs:s val="${fn:length(invalidLocks)}"/>:
    <c:choose>
      <c:when test="${showMode == inplaceMode}">
        <%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
        <c:url var="resourcesUrl" value="/admin/editProject.html?projectId=${project.externalId}&tab=JetBrains.SharedResources"/>
        <ul>
          <c:forEach items="${invalidLocks}" var="item">
            <li><a href="${resourcesUrl}"><c:out value="${item.key.name}"/></a> &mdash; <c:out value="${item.value}"/></li>
          </c:forEach>
        </ul>
      </c:when>
      <c:otherwise>
        <%
          @SuppressWarnings("unchecked")
          final Set<Lock> locks = ((Map<Lock, String>)healthStatusItem.getAdditionalData().get("invalid_locks")).keySet();
        %>
        <c:out value="<%=ResourceHelper.formatLocksList(locks)%>"/>
      </c:otherwise>
    </c:choose>
  </div>
</c:if>