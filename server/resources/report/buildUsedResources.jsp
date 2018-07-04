<%--suppress ELValidationInJSP --%>
<%--
  ~ Copyright 2000-2018 JetBrains s.r.o.
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
<%@ page import="jetbrains.buildServer.sharedResources.model.resources.ResourceType" %>
<%@ page import="jetbrains.buildServer.sharedResources.model.LockType" %>

<c:set var="CUSTOM" value="<%=ResourceType.CUSTOM.name()%>"/>
<c:set var ="TYPE_READ" value="<%=LockType.READ.getName()%>"/>
<c:set var ="TYPE_WRITE" value="<%=LockType.WRITE.getName()%>"/>

<bs:linkCSS>
  /css/buildLog/buildParameters.css
</bs:linkCSS>
<c:choose>
  <%--@elvariable id="usedResources" type="java.util.List<jetbrains.buildServer.sharedResources.server.report.UsedResource>"--%>
  <%--@elvariable id="resourceOrigins" type="java.util.Map<java.lang.String, jetbrains.buildServer.serverSide.SProject>"--%>
  <%--@elvariable id="parameters" type="java.util.List<com.intellij.openapi.util.Pair<java.lang.String, java.lang.String>>"--%>
  <c:when test="${not empty usedResources}">
    <div class="buildParameters">
      <h2>Shared resources used by the build</h2>
      <table class="runnerFormTable" style="width: 80em;">
        <tr>
          <th style="width: 25%; white-space: nowrap">Resource</th>
          <th>Lock</th>
        </tr>
        <c:forEach var="ur" items="${usedResources}">
          <c:set var="rc" value="${ur.resource}"/>
          <tr>
            <td style="white-space: nowrap">
              <c:set var="tooltipContent" value=""/>
              <c:set var="project" value="${resourceOrigins[rc.name]}"/>
              <c:set var="resourceNameContent">
                <c:choose>
                  <c:when test="${not empty project}">
                    <authz:authorize projectId="${project.externalId}" allPermissions="EDIT_PROJECT">
                      <jsp:attribute name="ifAccessGranted"> <%--edit project--%>
                        <admin:editProjectLink projectId="${project.externalId}" addToUrl="&tab=JetBrains.SharedResources"><bs:out value="${project.name} / ${rc.name}"/></admin:editProjectLink>
                      </jsp:attribute>
                      <jsp:attribute name="ifAccessDenied"> <%--view project--%>
                        <bs:projectLink project="${project}"><bs:out value="${project.name} / ${rc.name}"/></bs:projectLink>
                      </jsp:attribute>
                    </authz:authorize>
                  </c:when>
                  <c:otherwise>
                    <bs:out value="${rc.name}"/>
                  </c:otherwise>
                </c:choose>
              </c:set>
              <c:choose>
                <c:when test="${not empty rc}">
                  <c:set var="containerId" value="resource_${util:forJSIdentifier(rc.name)}"/>
                  <c:choose>
                    <c:when test="${rc.type == CUSTOM}">
                      <div id="${containerId}" style="display:none">
                        <bs:out value="Resource with custom values: "/>
                        <ul>
                          <c:forEach items="${rc.values}" var="val">
                            <li><bs:out value="${val}"/></li>
                          </c:forEach>
                        </ul>
                      </div>
                    </c:when>
                    <c:otherwise>
                      <div id="${containerId}" style="display:none">
                        <c:choose>
                          <c:when test="${rc.quota == -1}">
                            <bs:out value="Resource with infinite quota"/><br/>
                          </c:when>
                          <c:otherwise>
                            <bs:out value="Resource with quota of ${rc.quota}"/><br/>
                          </c:otherwise>
                        </c:choose>
                      </div>
                    </c:otherwise>
                  </c:choose>
                  <c:set var="tooltipContent">
                    <bs:tooltipAttrs containerId="${containerId}"/>
                  </c:set>
                </c:when>
                <c:otherwise/>
              </c:choose>
              <span ${tooltipContent}>${resourceNameContent}</span>
            </td>
            <td>
              <c:forEach items="${ur.locks}" var="lock">
                <c:choose>
                  <c:when test="${rc.type == CUSTOM}">
                    <c:choose>
                      <c:when test="${lock.type.name == TYPE_READ}">
                        Locked value: <code><bs:out value="${lock.value}"/></code><br/>
                      </c:when>
                      <c:otherwise>
                        <bs:out value="All custom values were locked"/>
                      </c:otherwise>
                    </c:choose>
                  </c:when>
                  <c:otherwise>
                    <bs:out value="${lock.type.descriptiveName}"/><br/>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </td>
          </tr>
        </c:forEach>
      </table>

      <c:if test="${not empty parameters}">
        <h2>Provided build parameters</h2>
        <table class="runnerFormTable" style="width: 80em;">
          <tr>
            <th style="width: 25%; white-space: nowrap">Name</th>
            <th>Value</th>
          </tr>
          <c:forEach items="${parameters}" var="pair">
            <c:set var="val" value="${empty pair.second ? '<empty>' : pair.second}"/>
            <c:set var="valueClass" value="${empty pair.second ? 'emptyValue' : ''}"/>
            <tr>
              <td class="at_top"><c:out value="${pair.first}"/></td>
              <td class="${valueClass}"><bs:out value="${val}" multilineOnly="true"/></td>
            </tr>
          </c:forEach>
        </table>
      </c:if>
    </div>
  </c:when>
  <c:otherwise>
    <bs:out value="No resources were locked by the current build"/>
  </c:otherwise>
</c:choose>



