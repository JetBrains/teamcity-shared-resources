<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>
<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
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

<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<jsp:useBean id="showMode" type="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" scope="request"/>
<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>

<%--@elvariable id="p" type="jetbrains.buildServer.serverSide.SProject"--%>
<%--@elvariable id="invalidResources" type="java.util.Map<java.lang.String, java.util.List<java.lang.String>>"--%>
<c:set var="invalidResources" value="${healthStatusItem.additionalData['invalidResources']}"/>
<c:set var="p" value="${healthStatusItem.additionalData['project']}"/>

<c:if test="${not empty invalidResources}">
  <div>
    <bs:projectLink project="${p}">
      <bs:out value="${p.extendedName}" />
    </bs:projectLink> contains project feature<bs:s val="${fn:length(invalidResources)}"/> with invalid resource definition<bs:s val="${fn:length(invalidResources)}"/>:
    <ul>
      <c:forEach var="res" items="${invalidResources}">
        <bs:out value="${res.key}"/>
        <ul>
          <c:forEach var="err" items="${res.value}">
            <li><bs:out value="${err}"/></li>
          </c:forEach>
        </ul>
      </c:forEach>
    </ul>
  </div>
</c:if>