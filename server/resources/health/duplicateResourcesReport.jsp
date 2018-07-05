<%@ page import="java.util.List" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<jsp:useBean id="showMode" type="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" scope="request"/>
<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>

<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>
<%--@elvariable id="p" type="jetbrains.buildServer.serverSide.SProject"--%>
<c:set var="p" value="${healthStatusItem.additionalData['project']}"/>
<%--@elvariable id="dups" type="java.util.List<java.lang.String>"--%>
<c:set var="dups" value="${healthStatusItem.additionalData['duplicates']}"/>
<c:if test="${not empty dups}">
  <div>
    <authz:authorize projectId="${p.externalId}" allPermissions="EDIT_PROJECT" >
          <jsp:attribute name="ifAccessGranted">
            <c:url var="editUrl" value="/admin/editProject.html?projectId=${p.externalId}&tab=JetBrains.SharedResources"/>
          <a href="${editUrl}"><c:out value="${p.extendedFullName}"/></a>
          </jsp:attribute>
          <jsp:attribute name="ifAccessDenied">
            <bs:projectLink project="${p}"><c:out value="${p.extendedFullName}"/></bs:projectLink>
          </jsp:attribute>
    </authz:authorize> contains duplicate resource  shared resources definition<bs:s val="${fn:length(dups)}"/>:
    <%
      @SuppressWarnings("unchecked")
      final List<String> dups = (List<String>)healthStatusItem.getAdditionalData().get("duplicates");
    %>
    <%=dups.stream().collect(Collectors.joining(", ", "\"", "\""))%>
  </div>
</c:if>