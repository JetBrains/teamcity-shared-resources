<%@ page import="java.util.List" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<%--
  ~ Copyright 2000-2022 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

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
    </authz:authorize> contains duplicate shared resources definition<bs:s val="${fn:length(dups)}"/>:
    <%
      @SuppressWarnings("unchecked")
      final List<String> dups = (List<String>)healthStatusItem.getAdditionalData().get("duplicates");
      final String dupsFormatted = dups.stream().collect(Collectors.joining(", ", "\"", "\""));
    %>
    <c:out value="<%=dupsFormatted%>"/>
  </div>
</c:if>