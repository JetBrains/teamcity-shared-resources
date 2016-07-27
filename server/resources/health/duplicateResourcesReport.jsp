<%@ page import="java.util.List" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>
<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<jsp:useBean id="showMode" type="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" scope="request"/>
<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>
<%--@elvariable id="p" type="jetbrains.buildServer.serverSide.SProject"--%>
<c:set var="p" value="${healthStatusItem.additionalData['project']}"/>
<%--@elvariable id="dups" type="java.util.List<java.lang.String>"--%>
<c:set var="dups" value="${healthStatusItem.additionalData['duplicates']}"/>
<c:if test="${not empty dups}">
  <div>
    <bs:projectLink project="${p}">
      <bs:out value="${p.extendedName}" />
    </bs:projectLink> contains duplicate resource definition<bs:s val="${fn:length(dups)}"/>:
    <%
      @SuppressWarnings("unchecked")
      final List<String> dups = (List<String>)healthStatusItem.getAdditionalData().get("duplicates");
    %>
    <%=dups.stream().collect(Collectors.joining(", "))%>
  </div>
</c:if>