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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants" %>
<%@ page import="jetbrains.buildServer.sharedResources.server.feature.FeatureParams" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="project" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="locks" scope="request" type="java.util.Map<java.lang.String, jetbrains.buildServer.sharedResources.model.Lock>"/>
<jsp:useBean id="bean" scope="request" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean"/>

<c:set var="locksFeatureParamKey" value="<%=FeatureParams.LOCKS_FEATURE_PARAM_KEY%>"/>

<c:set var="PARAM_RESOURCE_NAME" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME%>"/>
<c:set var="PARAM_PROJECT_ID" value="<%=SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID%>"/>
<c:set var="PARAM_RESOURCE_QUOTA" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA%>"/>

<c:set var="ACTION_ADD" value="<%=SharedResourcesPluginConstants.WEB.ACTION_ADD%>"/>


<script type="text/javascript">
//noinspection JSValidateTypes
BS.LocksDialog = OO.extend(BS.AbstractModalDialog, {
  attachedToRoot: false,
  myData: {}, // here we have locks
  myLocksDisplay: {},
  editMode: false,
  currentLockName: "",
  existingResources: {}, // here are existing resources + resources created in place

  fillData: function() {
    <c:forEach var="item" items="${locks}">
    this.myData['${item.key}'] = '${item.value.type.name}';
    </c:forEach>
    <c:forEach var="item" items="${bean.resources}">
    this.existingResources['${item.name}'] = true;
    </c:forEach>

    this.myLocksDisplay['readLock'] = "Read lock";
    this.myLocksDisplay['writeLock'] = "Write lock";
  },

  refreshUI: function() {
    var tableBody = $j('#locksTaken tbody:last');
    var textArea = $('${locksFeatureParamKey}');
    tableBody.children().remove();
    var self = this.myData;
    var textAreaContent = "";
    var size = _.size(self);

    if (size > 0) {
      BS.Util.show('locksTaken');
      BS.Util.hide('noLocksTaken');
      for (var key in self) {
        if (self.hasOwnProperty(key)) {
          var onclick = this.generateOnClickAction(key);
          <%-- override default style, as we are in the table with disabled border-top --%>
          var content = "<tr style=\"border-top: 1px solid #CCC\"><td class=\"highlight\" onclick=" + onclick + ">" + key + "</td><td class=\"highlight\" onclick=" + onclick + ">" + this.myLocksDisplay[self[key]] +"</td>";
          content += "<td class=\"edit highlight\" onclick=" + onclick + "><a href=\"#\" onclick=\"BS.LocksDialog.showEdit(\'" + key + "\'); return false\">edit</a></td>";
          content += "<td class=\"edit\"><a href=\"#\" onclick=\"BS.LocksDialog.deleteLockFromTakenLocks(\'" + key + "\'); return false\">delete</a></td>";
          content += "</tr>";
          textAreaContent += key + " " + self[key] + "\n";
          tableBody.append(content);
        }
      }
    } else {
      BS.Util.hide('locksTaken');
      BS.Util.show('noLocksTaken');
    }
    textArea.value = textAreaContent.trim();
    var resourceDropdown = $j('#lockFromResources');
    resourceDropdown.children().remove();
    for (var resource in this.existingResources) {
      resourceDropdown.append("<option value='" + resource + "'>" + resource + "</option>");
    }

    // here rebuild highlighted rows
    var highlightableElements = $j("#locksTaken td.highlight");
    highlightableElements.each(function(i, element) {
      BS.TableHighlighting.createInitElementFunction.call(this, element, 'Click to edit lock');
    });

    BS.MultilineProperties.updateVisible();
  },

  generateOnClickAction: function(key) {
    return "BS.LocksDialog.showEdit(\'" + key + "\'); return false;";
  },

  getContainer: function() {
    return $('locksDialog');
  },

  showDialog: function() {
    this.editMode = false;
    $j("#locksDialogSubmit").prop('value', 'Add');
    $j('#newLockName').val("");
    $j('#resource_quota').value = "1";

    // Set dialog mode to choose
    $j('#lockSource option').each(function() {
      var self = $j(this);
      self.prop("selected", self.val() == 'choose');
    });
    this.syncResourceSelectionState();

    this.refreshUI();
    this.showCentered();
    this.bindCtrlEnterHandler(this.submit.bind(this));
  },

  /**
   * Shows dialog used to edit lock
   * @param lockName name of the lock
   */
  showEdit: function(lockName) {
    this.editMode = true;
    this.currentLockName = lockName;
    $j("#locksDialogSubmit").prop('value', 'Save');
    $j('newLockName').val("");
    var lockType = this.myData[lockName];
    $j('#lockSource option').each(function() {
      var self = $j(this);
      self.prop("selected", self.val() == 'choose');
    }); // restore 'resource is chosen' state
    this.syncResourceSelectionState();

    $j('#lockFromResources option').each(function() {
      var self = $j(this);
      self.prop("selected", self.val() == lockName);
    }); // restore lock name in selection

    $j('#newLockType option').each(function() {
      var self = $j(this);
      self.prop("selected", self.val() == lockType);
    }); // restore lock type

    this.showCentered();
    this.bindCtrlEnterHandler(this.submit.bind(this));
  },

  submit: function() {
    if (!this.validate()) return false;

    var flag = $j('#lockSource option:selected').val();
    var lockType = $j('#newLockType option:selected').val();

    if (this.editMode) {
      delete this.myData[this.currentLockName];
      this.currentLockName = "";
    }

    var lockName;
    if (flag === 'choose') {
      lockName = $j('#lockFromResources option:selected').val();
      this.myData[lockName] = lockType;
    } else if (flag === 'create') {
      lockName = $j('#newLockName').val();
      if (!this.existingResources[lockName]) { // if resource does not exist
        // ajax to add resource
        var quota;
        if ($j('#use_quota').is(':checked')) {
          quota = $j('#resource_quota').val();
        }
        this.createResourceInPlace(lockName, quota);
        // add to existing resources if success (perhaps re-read all model)
        this.existingResources[lockName] = true;
      }
      // choose newly created resource
      this.myData[lockName] = lockType;
    }



    this.refreshUI();
    this.close();
    return false;
  },

  clearErrors: function () {
    BS.Util.hide('error_Name');
    $j('#error_Name').html("");
    BS.Util.hide('error_Quota');
    $j('#error_Quota').html("");
  },

  validate: function() { // todo: add validation to choose
    // clean and hide errors here
    this.clearErrors();
    var errorsPresent = false;
    var flag = $j('#lockSource option:selected').val();
    if (flag === 'create') {
      // check name
      var element = $j('#newLockName');
      var value =  element.val().trim();

      if (value.length === 0) {
        BS.Util.show('error_Name');
        $j('#error_Name').html("Name must not be empty");
        // check for existence
        errorsPresent = true;
      }
      element.val(value);

      // check quota
      if ($j('#use_quota').is(':checked')) {
        element = $j('#resource_quota');
        value = element.val().trim();
        if (!value.match(/^[0-9]+$/)) {
          BS.Util.show('error_Quota');
          $j('#error_Quota').html("Quota value is not valid");
          errorsPresent = true;
        }
        element.val(value);
      }
    } else if (flag === 'choose') {
      var lockName = $j('#lockFromResources option:selected').val();
      if (!lockName) {
        errorsPresent = true;
        alert('Please create a resource to lock');
      }
    } else {
      errorsPresent = true;
    }
    return !errorsPresent;
  },

  deleteLockFromTakenLocks: function(lockName) {
    delete this.myData[lockName];
    this.refreshUI();
  },

  syncResourceSelectionState: function() {
    var flag = $j('#lockSource option:selected').val();
    if (flag === 'choose')  {
      this.toggleModeChoose();
    } else if (flag === 'create') {
      this.toggleModeCreate();
    }
    this.clearErrors();
    BS.MultilineProperties.updateVisible();
  },

  toggleModeChoose: function() {
    // choose: show
    BS.Util.show('row_resourceChoose');
    // create: hide
    BS.Util.hide('row_resourceCreate');
    BS.Util.hide('row_useQuotaSwitch');
    BS.Util.hide('row_useQuotaInput');
  },

  toggleModeCreate: function() {
    // choose: hide
    BS.Util.hide('row_resourceChoose');
    // create: show
    BS.Util.show('row_resourceCreate');
    BS.Util.show('row_useQuotaSwitch');
    // quota: hide
    $j('#use_quota').removeAttr('checked');
    BS.Util.hide('row_useQuotaInput');
  },

  toggleUseQuota: function() {
    if ($j('#use_quota').is(':checked')) {
      BS.Util.show('row_useQuotaInput');
    } else {
      BS.Util.hide('row_useQuotaInput');
    }

    BS.MultilineProperties.updateVisible();
  },


  //todo:  refactor JS
  createResourceInPlace: function(resource_name, quota) {
    var addUrl = window['base_uri'] + "${ACTION_ADD}";
    if (quota) {
      BS.ajaxRequest(addUrl, {
        parameters: {
          '${PARAM_PROJECT_ID}':'${project.projectId}',
          '${PARAM_RESOURCE_NAME}': resource_name,
          '${PARAM_RESOURCE_QUOTA}': quota
        }
      });
    } else {
      BS.ajaxRequest(addUrl, {
        parameters: {
          '${PARAM_PROJECT_ID}':'${project.projectId}',
          '${PARAM_RESOURCE_NAME}':resource_name
        }
      });
    }
  }
});
</script>

<tr>
  <td colspan="2" style="padding-right: 8px">
    <table id="locksTaken" class="parametersTable">
      <thead>
      <tr>
        <th class="70%">Lock Name</th>
        <th colspan="3" style="width: 30%">Lock Type</th>
      </tr>
      </thead>
      <tbody>
      </tbody>
    </table>
    <div id="noLocksTaken" style="display: none">
      No locks are currently defined
    </div>
  </td>
</tr>

<script type="text/javascript">
  BS.LocksDialog.fillData();
  BS.LocksDialog.refreshUI();
</script>

<tr style="display: none">
  <th><label for="${locksFeatureParamKey}">Resource name:</label></th>
  <td>
    <props:multilineProperty name="${locksFeatureParamKey}" linkTitle="Enter shared resource name(s)" cols="49" rows="5" expanded="${false}"/>
    <span class="error" id="error_${locksFeatureParamKey}"></span>
    <span class="smallNote">Please specify shared resources that must be locked during build</span>
  </td>
</tr>

<tr>
  <td class="noBorder" colspan="2">
    <forms:addButton id="addNewLock" onclick="BS.LocksDialog.showDialog(); return false">Add lock</forms:addButton>
    <bs:dialog dialogId="locksDialog" title="Lock Management" closeCommand="BS.LocksDialog.close()">
      <table class="runnerFormTable">
        <tr>
          <th><label for="lockSource">Resource selection: </label></th>
          <td>
            <forms:select name="lockSource" id="lockSource" style="width: 90%" onchange="BS.LocksDialog.syncResourceSelectionState(); return true;">
              <forms:option value="create">Create new resource</forms:option>
              <forms:option value="choose">Choose an existing resource</forms:option>
            </forms:select>
            <span class="smallNote">Choose whether you want to create a new shared resource or use an existing one</span>
          </td>
        </tr>
        <tr id="row_resourceChoose">
          <th><label for="lockFromResources">Resource name:</label></th>
          <td>
            <c:choose>
              <c:when test="${not empty bean.resources}">
                <forms:select name="lockFromResources" id="lockFromResources" style="width: 90%"/>
                <span class="smallNote">Choose the resource you want to lock</span>
              </c:when>
              <c:otherwise>
                <c:out value="No resources available. Please add the resource you want to lock."/>
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
        <tr id="row_resourceCreate">
          <th><label for="newLockName">Resource name:</label></th>
          <td>
            <forms:textField id="newLockName" name="newLockName" style="width: 90%" maxlength="40" className="longField buildTypeParams" defaultText=""/>
            <span class="error" id="error_Name"></span>
            <span class="smallNote">Specify the name of resource</span>
          </td>
        </tr>
        <tr id="row_useQuotaSwitch">
          <th>Use quota:</th>
          <td>
            <forms:checkbox name="use_quota" id="use_quota" onclick="BS.LocksDialog.toggleUseQuota()" checked="false"/>
            <span class="smallNote">Quota is a number of concurrent read locks that can be acquired on resource</span>
          </td>
        </tr>
        <tr id="row_useQuotaInput" style="display: none">
          <th><label for="resource_quota">Resource quota:</label> </th>
          <td>
            <forms:textField name="resource_quota" style="width: 25%" id="resource_quota" className="longField buildTypeParams" maxlength="3"/>
            <span class="error" id="error_Quota"></span>
          </td>
        </tr>
        <tr>
          <th><label for="newLockType">Lock type:</label></th>
          <td>
            <forms:select name="newLockType" id="newLockType" style="width: 90%">
              <forms:option value="readLock">Read Lock</forms:option>
              <forms:option value="writeLock">Write Lock</forms:option>
            </forms:select>
            <span class="smallNote">Select type of lock: read lock (shared), or write lock (exclusive)</span>
          </td>
        </tr>
      </table>
      <div class="popupSaveButtonsBlock">
        <forms:cancel onclick="BS.LocksDialog.close()" showdiscardchangesmessage="false"/>
        <forms:submit id="locksDialogSubmit" type="button" label="Add Lock" onclick="BS.LocksDialog.submit()"/>
      </div>
    </bs:dialog>
  </td>
</tr>


