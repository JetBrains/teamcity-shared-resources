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
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants" %>
<%@ page import="jetbrains.buildServer.sharedResources.model.resources.ResourceType" %>

<jsp:useBean id="project" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="bean" scope="request" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean"/>

<c:set var="PARAM_RESOURCE_NAME" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME%>"/>
<c:set var="PARAM_PROJECT_ID" value="<%=SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID%>"/>
<c:set var="PARAM_RESOURCE_QUOTA" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA%>"/>
<c:set var="PARAM_RESOURCE_TYPE" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE%>"/>
<c:set var="PARAM_RESOURCE_VALUES" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_VALUES%>"/>
<c:set var="PARAM_OLD_RESOURCE_NAME" value="<%=SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME%>"/>

<c:set var="ACTION_ADD" value="<%=SharedResourcesPluginConstants.WEB.ACTION_ADD%>"/>
<c:set var="ACTION_EDIT" value="<%=SharedResourcesPluginConstants.WEB.ACTION_EDIT%>"/>
<c:set var="ACTION_DELETE" value="<%=SharedResourcesPluginConstants.WEB.ACTION_DELETE%>"/>

<c:set var="type_quota" value="<%=ResourceType.QUOTED%>"/>
<c:set var="type_custom" value="<%=ResourceType.CUSTOM%>"/>


<c:url var="url" value="editProject.html?projectId=${project.projectId}&tab=JetBrains.SharedResources"/>


<script type="text/javascript">

  BS.SharedResourcesActions = {
    getCommonParams: function() {
      // if quota checkbox in unchecked, send no quota info
      var type = $j('#resource_type option:selected').val();
      var params = {};
      params['${PARAM_PROJECT_ID}'] = '${project.projectId}';
      params['${PARAM_RESOURCE_NAME}'] = $j('#resource_name').val();
      // infinite
      if (type === 'infinite') {
        params['${PARAM_RESOURCE_TYPE}'] = 'quoted';
      }
      // quoted
      if (type === 'quoted') {
        params['${PARAM_RESOURCE_TYPE}'] = 'quoted';
        params['${PARAM_RESOURCE_QUOTA}'] = $j('#resource_quota').val();
      }
      // custom
      if (type === 'custom') {
        params['${PARAM_RESOURCE_TYPE}'] = 'custom';
        params['${PARAM_RESOURCE_VALUES}'] = $j('#customValues').val();
      }
      return params;
    },

    addUrl: window['base_uri'] + "${ACTION_ADD}",
    addResource: function() {
      var params = this.getCommonParams();
      BS.ajaxRequest(this.addUrl, {
        parameters: params,
        onSuccess: function() {
          window.location.reload();
        }
      });
    },

    editUrl: window['base_uri'] + "${ACTION_EDIT}",
    editResource: function(old_resource_name) {
      var params = this.getCommonParams();
      params['${PARAM_OLD_RESOURCE_NAME}'] = old_resource_name;
      BS.ajaxRequest(this.editUrl, {
        parameters: params,
        onSuccess: function() {
          window.location.reload();
        }
      });
    },

    deleteUrl: window['base_uri'] + "${ACTION_DELETE}",
    deleteResource: function(resource_name) {
      var params = {
        '${PARAM_PROJECT_ID}':'${project.projectId}',
        '${PARAM_RESOURCE_NAME}': resource_name
      };

      if (confirm('Are you sure you want to delete this resource?')) {
        BS.ajaxRequest(this.deleteUrl, {
          parameters: params,
          onSuccess: function() {
            window.location.reload();
          }
        });
      }
    },
    alertCantDelete: function(resource_name) {
      alert('Resource ' + resource_name + " can't be deleted because it is in use");
    }

  };


</script>


<script type="text/javascript">
  var myValues;
  var r;
  var v;
  <c:forEach var="item" items="${bean.resources}">
  <c:set var="type" value="${item.type}"/>
  r = {
    name: '${item.name}',
    type: '${item.type}'
  };
  <c:choose>

  <%-- quoted resource--%>
  <c:when test="${type == type_quota}">
  r['quota'] = '${item.quota}';
  r['infinite'] = ${item.infinite};
  BS.ResourceDialog.myData['${item.name}'] = r;
  </c:when>

  <%-- custom resource--%>
  <c:when test="${type == type_custom}">
  myValues = [];
  <c:forEach items="${item.values}" var="val">
  myValues.push('${val}');
  </c:forEach>
  r['customValues'] = myValues;
  BS.ResourceDialog.myData['${item.name}'] = r;
  </c:when>

  <c:otherwise>
  console.log('Resource [${item.name}] was not recognized');
  </c:otherwise>
  </c:choose>
  BS.ResourceDialog.existingResources['${item.name}'] = true;
  </c:forEach>
</script>

<div>
  <p>
    <forms:addButton id="addNewResource" onclick="BS.ResourceDialog.showDialog(); return false">Add new resource</forms:addButton>
  </p>
  <bs:dialog dialogId="resourceDialog" titleId="resourceDialogTitle"
             title="Resource Management" closeCommand="BS.ResourceDialog.close()">
    <table class="runnerFormTable">
      <tr>
        <th><label for="resource_name">Resource name:</label></th>
        <td>
          <forms:textField name="resource_name" id="resource_name" style="width: 100%" className="longField buildTypeParams" maxlength="40"/>
          <span class="error" id="error_Name"></span>
          <span class="smallNote">Specify the name of resource</span>
        </td>
      </tr>
      <tr>
        <th>Resource type:</th>
        <td>
          <forms:select name="resoruce_type" id="resource_type" style="width: 90%" onchange="BS.ResourceDialog.syncResourceSelectionState(); return true;">
            <forms:option value="infinite">Infinite resource</forms:option>
            <forms:option value="quoted">Resource with quota</forms:option>
            <forms:option value="custom">Resource with custom values</forms:option>
          </forms:select>
        </td>
      </tr>
      <tr id="quota_row" style="display: none">
        <th><label for="resource_quota">Resource quota:</label> </th>
        <td>
          <forms:textField name="resource_quota" style="width: 25%" id="resource_quota" className="longField buildTypeParams" maxlength="3"/>
          <span class="error" id="error_Quota"></span>
          <span class="smallNote">Quota is a number of concurrent read locks that can be acquired on resource</span>
        </td>
      </tr>
      <tr id="custom_row" style="display: none">
        <th>Custom values: </th>
        <td>
          <props:textarea name="customValues" textAreaName="customValuesArea" value=""
                          linkTitle="Define custom values" cols="30" rows="5" expanded="${true}"/>
          <span class="error" id="error_Values"></span>
          <span class="smallNote">Define one custom value for resource per line</span>
        </td>
      </tr>
    </table>
    <div class="popupSaveButtonsBlock">
      <forms:cancel onclick="BS.ResourceDialog.close()" showdiscardchangesmessage="false"/>
      <forms:submit id="resourceDialogSubmit" type="button" label="Add Resource" onclick="BS.ResourceDialog.submit()"/>
    </div>
  </bs:dialog>


  <c:choose>
    <c:when test="${not empty bean.resources}">
      <l:tableWithHighlighting style="width: 70%"
                               id="resourcesTable"
                               className="parametersTable"
                               mouseovertitle="Click to edit resource"
                               highlightImmediately="true">
        <tr>
          <th>Resource name</th>
          <th style="width:55%" colspan="4">Resource description</th>
        </tr>
        <c:forEach var="resource" items="${bean.resources}">
          <c:set var="onclick" value="BS.ResourceDialog.showEdit('${resource.name}');"/>
          <c:set var="resourceName" value="${resource.name}"/>
          <c:set var="usage" value="${bean.usageMap[resourceName]}"/> <%--Map<SBuildType -> LockType>--%>
          <c:set var="used" value="${not empty usage}"/>
          <tr>
            <td class="name highlight" onclick="${onclick}"><c:out value="${resourceName}"/></td>
            <c:choose>
              <c:when test="${resource.type == type_quota}">
                <c:choose>
                  <c:when test="${resource.infinite}">
                    <td class="highlight" onclick="${onclick}">Quota: Infinite</td>
                  </c:when>
                  <c:otherwise>
                    <td class="highlight" onclick="${onclick}">Quota: <c:out value="${resource.quota}"/></td>
                  </c:otherwise>
                </c:choose>
              </c:when>
              <c:when test="${resource.type == type_custom}">
                <td class="highlight" onclick="${onclick}">Custom values</td>
              </c:when>
            </c:choose>
            <c:choose>
              <c:when test="${used}">
                <td class="highlight" onclick="${onclick}">
                  <c:set var="usageSize" value="${fn:length(usage)}"/>
                  <bs:simplePopup controlId="${fn:replace(resourceName, ' ', '_')}"
                                  linkOpensPopup="false"
                                  popup_options="shift: {x: -150, y: 20}, className: 'quickLinksMenuPopup'">
                    <jsp:attribute name="content">
                      <div>
                        <ul class="menuList">
                          <c:forEach items="${usage}" var="usedInBuildType">
                            <c:set var="buildType" value="${usedInBuildType.key}"/>
                            <c:set var="lockType" value="${usedInBuildType.value}"/>
                            <admin:editBuildTypeNavSteps settings="${buildType}"/>
                            <jsp:useBean id="buildConfigSteps" scope="request" type="java.util.ArrayList<jetbrains.buildServer.controllers.admin.projects.ConfigurationStep>"/>
                            <l:li>
                              <admin:editBuildTypeLink buildTypeId="${buildType.buildTypeId}" step="${buildConfigSteps[2].stepId}" cameFromUrl="${url}">
                                <span style="white-space: nowrap">
                                  ${buildType.name} (${lockType.descriptiveName})
                                </span>
                              </admin:editBuildTypeLink>
                            </l:li>
                          </c:forEach>
                        </ul>
                      </div>
                    </jsp:attribute>
                    <jsp:body>Used in ${usageSize} build configuration<bs:s val="${usageSize}"/></jsp:body>
                  </bs:simplePopup>
                </td>
              </c:when>
              <c:otherwise>
                <td class="highlight" onclick="${onclick}">Resource is not used</td>
              </c:otherwise>
            </c:choose>
            <td class="edit highlight" onclick="${onclick}"><a href="#">edit</a></td>
            <td class="edit">
              <c:choose>
                <c:when test="${used}">
                  <a href="#" onclick="BS.SharedResourcesActions.alertCantDelete('${resource.name}')">delete</a>
                </c:when>
                <c:otherwise>
                  <a href="#" onclick="BS.SharedResourcesActions.deleteResource('${resource.name}')">delete</a>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </c:forEach>
      </l:tableWithHighlighting>
    </c:when>
    <c:otherwise>
      <p>
        <c:out value="There are no resources defined"/>
      </p>
    </c:otherwise>
  </c:choose>

</div>

