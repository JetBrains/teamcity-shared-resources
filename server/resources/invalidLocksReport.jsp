<%--
  ~ Copyright 2000-2013 JetBrains s.r.o.
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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/include-internal.jsp" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>

<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<jsp:useBean id="showMode" type="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" scope="request"/>

<c:set var="invalidLocks" value="${healthStatusItem.additionalData['invalid_locks']}"/>
<c:set var="buildType" value="${healthStatusItem.additionalData['build_type']}"/>
<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>


<c:choose>
  <c:when test="${showMode == inplaceMode}">
    <c:if test="${not empty invalidLocks}">
      <div>
        <bs:out value="${buildType.name}"/> contains invalid lock<bs:s val="${fn:length(invalidLocks)}"/>:
          ${fn:join(invalidLocks, ", ")}
      </div>
    </c:if>
  </c:when>
  <c:otherwise>
    <div>
      <c:if test="${not empty invalidLocks}">
        <bs:buildTypeLink buildType="${buildType}"/>&nbsp;contains invalid lock<bs:s val="${fn:length(invalidLocks)}"/>:
        ${fn:join(invalidLocks, ", ")}
      </c:if>
    </div>
  </c:otherwise>
</c:choose>