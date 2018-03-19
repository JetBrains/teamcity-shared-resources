<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ page import="jetbrains.buildServer.sharedResources.model.resources.ResourceType" %>
<%@ page import="jetbrains.buildServer.sharedResources.model.LockType" %>
<%@ include file="/include-internal.jsp" %>
<%@ page contentType="text/html;charset=UTF-8" %>
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

<%--@elvariable id="locks" type="java.util.Map<java.lang.String, jetbrains.buildServer.sharedResources.model.Lock>"--%>
<%--@elvariable id="resources" type="java.util.Map<java.lang.String, jetbrains.buildServer.sharedResources.model.resources.Resource>"--%>
<%--@elvariable id="resourceOrigins" type="java.util.Map<java.lang.String, jetbrains.buildServer.serverSide.SProject>"--%>
<c:set var="CUSTOM" value="<%=ResourceType.CUSTOM.name()%>"/>
<c:set var ="TYPE_READ" value="<%=LockType.READ.getName()%>"/>
<c:set var ="TYPE_WRITE" value="<%=LockType.WRITE.getName()%>"/>

<bs:linkCSS>
  /css/buildLog/buildParameters.css
</bs:linkCSS>

<c:choose>
  <c:when test="${not empty locks}">
    <div class="buildParameters">
      <h2>Shared resources used by the build</h2>
      <table class="runnerFormTable"> 
        <tr>
          <th style="width: 30%">Resource</th>
          <th>Lock</th>
        </tr>
        <c:forEach var="item" items="${locks}">
          <c:set var="rc" value="${resources[item.key]}"/>
          <tr>
            <td>   <%--render resource--%>
              <c:set var="tooltipContent" value=""/>
                <%-- test for project--%>
              <c:set var="project" value="${resourceOrigins[rc.name]}"/>
              <c:set var="resourceNameContent">
                <c:choose>
                  <c:when test="${not empty project}">
                    <authz:authorize projectId="${project.externalId}" allPermissions="EDIT_PROJECT">
                      <jsp:attribute name="ifAccessGranted"> <%--edit project--%>
                        <admin:editProjectLink projectId="${project.externalId}" addToUrl="&tab=JetBrains.SharedResources"><bs:out value=" ${item.key}"/></admin:editProjectLink>
                      </jsp:attribute>
                      <jsp:attribute name="ifAccessDenied"> <%--view project--%>
                        <bs:projectLink project="${project}"><bs:out value="${item.key}"/></bs:projectLink>
                      </jsp:attribute>
                    </authz:authorize>
                  </c:when>
                  <c:otherwise>
                    <bs:out value="${item.key}"/>
                  </c:otherwise>
                </c:choose>
              </c:set>

              <c:choose>
                <c:when test="${not empty rc}">
                  <c:choose>
                    <c:when test="${rc.type == CUSTOM}">
                      <c:set var="containerId" value="resource_${util:forJSIdentifier(rc.name)}"/>
                      <div id="${containerId}" style="display:none">
                        <bs:out value="Resource with custom values: "/><br/>
                        <c:forEach items="${rc.values}" var="val">
                          <bs:out value="${val}"/><br/>
                        </c:forEach>
                      </div>
                      <c:set var="tooltipContent">
                        <bs:tooltipAttrs containerId="${containerId}"/>
                      </c:set>
                    </c:when>
                    <c:otherwise/>
                  </c:choose>
                </c:when>
                <c:otherwise/>
              </c:choose>
              <span ${tooltipContent}>${resourceNameContent}</span>
            </td> <%--render lock--%>
            <td>
              <c:choose>
                <c:when test="${rc.type == CUSTOM}">
                  <bs:out value="${item.value.value}"/>
                </c:when>
                <c:otherwise>
                  <bs:out value="${item.value.type.descriptiveName}"/>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </c:forEach>
      </table>
    </div>
  </c:when>
  <c:otherwise>
    No shared resources were locked by current build
  </c:otherwise>
</c:choose>

