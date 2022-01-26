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