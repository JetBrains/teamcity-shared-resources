<%@ page import="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants" %>
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="project" scope="request" type="jetbrains.buildServer.serverSide.SProject"/>
<jsp:useBean id="keys" class="jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="locks" scope="request" type="java.util.List"/>
<jsp:useBean id="bean" scope="request" type="jetbrains.buildServer.sharedResources.pages.SharedResourcesBean"/>

<c:set var="PARAM_RESOURCE_NAME" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME%>"/>
<c:set var="PARAM_PROJECT_ID" value="<%=SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID%>"/>
<c:set var="PARAM_RESOURCE_QUOTA" value="<%=SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA%>"/>


<script type="text/javascript">
//noinspection JSUnusedGlobalSymbols

Object.size = function(obj) {
  var size = 0, key;
  for (key in obj) {
    if (obj.hasOwnProperty(key)) {
      size++;
    }
  }
  return size;
};

BS.LocksDialog = OO.extend(BS.AbstractModalDialog, {
  attachedToRoot: false,
  myData: {}, // here we have locks
  myLocksDisplay: {},
  editMode: false,
  currentLockName: "",
  existingResources: {}, // here are existing resources + resources created in place

  fillData: function() {
    <c:forEach var="item" items="${locks}">
    this.myData['${item.name}'] = '${item.type.name}';
    </c:forEach>
    <c:forEach var="item" items="${bean.resources}">
    this.existingResources['${item.name}'] = true;
    </c:forEach>

    this.myLocksDisplay['readLock'] = "Read lock";
    this.myLocksDisplay['writeLock'] = "Write lock";

  },

  refreshUi: function() {
    var tableBody = $j('#locksTaken tbody:last');
    var textArea = $('${keys.locksFeatureParamKey}');
    tableBody.children().remove();
    var self = this.myData;
    var textAreaContent = "";
    var size = Object.size(self);

    if (size > 0) {
      BS.Util.show('LocksTaken');
      BS.Util.hide('noLocksTaken');
      for (var key in self) {
        //noinspection JSUnfilteredForInLoop
        var content = "<tr><td>" + key + "</td><td>" + this.myLocksDisplay[self[key]] +"</td>";
        content += "<td class=\"edit\"><a href=\"#\" onclick=\"BS.LocksDialog.showEdit(\'" + key + "\'); return false\">edit</a></td>";
        content += "<td class=\"edit\"><a href=\"#\" onclick=\"BS.LocksDialog.deleteLockFromTakenLocks(\'" + key + "\'); return false\">delete</a></td>";
        content += "</tr>";
        //noinspection JSUnfilteredForInLoop
        textAreaContent += key + " " + self[key] + "\n";
        //noinspection JSCheckFunctionSignatures
        tableBody.append(content);
      }
    } else {
      BS.Util.hide('locksTaken');
      BS.Util.show('noLocksTaken');
    }
    textArea.value = textAreaContent.trim();
    BS.MultilineProperties.updateVisible();
  },

  getContainer: function() {
    return $('locksDialog');
  },

  showDialog: function() {
    this.editMode = false;
    // Set button title to Add
    $j("#locksDialogSubmit").prop('value', 'Add');
    // Empty input box
    $('newLockName').value = "";
    $j('#resource_quota').value = "1";


    // Set dialog mode to choose
    $j('#lockSource option').each(function() {
      var self = $j(this);
      //noinspection JSUnresolvedFunction
      self.prop("selected", self.val() == 'choose');
    });
    this.syncResourceSelectionState();

    this.refreshUi();
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
    $('newLockName').value = lockName;
    var lockType = this.myData[lockName];
    $j('#lockSource option').each(function() { // todo: not sure about this
      var self = $j(this);
      //noinspection JSUnresolvedFunction
      self.prop("selected", self.val() == 'choose');
    }); // restore 'resource is chosen' state

    $j('#lockFromResources option').each(function() {
      var self = $j(this);
      //noinspection JSUnresolvedFunction
      self.prop("selected", self.val() == lockName);
    }); // restore lock name in selection

    $j('#newLockType option').each(function() {
      var self = $j(this);
      //noinspection JSUnresolvedFunction
      self.prop("selected", self.val() == lockType);
    }); // restore lock type
    this.showCentered();
    this.bindCtrlEnterHandler(this.submit.bind(this));
  },

  submit: function() {
    if (!this.validate()) return false;
    // todo: add resource creation here
    if (this.editMode) {
      // we change / add new resource through change
    } else {
      // we choose / add new resource through add
      var flag = $j('#lockSource option:selected').val();
      // get value lockType dropdown
      var lockType = $j('#newLockType option:selected').val();
      // if choose existing
      if (flag === 'choose') {
        // get value of lockName dropdown
        var lockName = $j('#lockFromResources option:selected').val();
        // update local model
        this.myData[lockName] = lockType;
        this.refreshUi();
        this.close();
        return false;
      } else if (flag === 'create') { // else if create
        var lockName = $('newLockName').value;
        if (!this.existingResources[lockName]) { // if resource does not exist
          // ajax to add resource
          var quota = undefined;
          if ($j('#use_quota').is(':checked')) {
            quota = $j('#resource_quota').val();
          }
          this.createResourceInPlace(lockName, quota);
          // add to existing resources if success (perhaps re-read all model)
          this.existingResources[lockName] = true;
        }
        // choose newly created resource
        this.myData[lockName] = lockType;
        this.refreshUi();
        this.close();
      }
    }
    //noinspection JSUnresolvedFunction
    if (this.editMode) {
      delete this.myData[this.currentLockName];
      this.currentLockName = "";
    }

    return false;
  },

  validate: function() { // todo: add validation to choose
    var flag = $j('#lockSource option:selected').val();
    if (flag === 'create') {
      var _name = $('newLockName').value;
      if (_name.length === 0) {
        alert('Please enter lock name');
        return false;
      }
    } else if (flag === 'choose') {
      var lockName = $j('#lockFromResources option:selected').val();
      if (!lockName) {
        alert('Please create a resource to lock');
        return false;
      }
    } else {
      return false;
    }
    return true;
  },

  deleteLockFromTakenLocks: function(lockName) {
    console.log("Deleting lock: [" + lockName + "]");
    delete this.myData[lockName];
    this.refreshUi();
  },

  syncResourceSelectionState: function() {
    //noinspection JSUnresolvedFunction
    var flag = $j('#lockSource option:selected').val(); // todo: add state syncing to dialog init
    if (flag === 'choose')  {
      this.toggleModeChoose();
    } else if (flag === 'create') {
      this.toggleModeCreate();
    }
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
      // show
      BS.Util.show('row_useQuotaInput');
    } else {
      //hide
      BS.Util.hide('row_useQuotaInput');
    }
    BS.MultilineProperties.updateVisible();
  },


  //todo:  refactor JS
  createResourceInPlace: function(resource_name, quota) {
    var addUrl = window['base_uri'] + "/sharedResourcesAdd.html";
    if (quota) {
      alert('Creating resource with quota');
      //noinspection JSDuplicatedDeclaration
      BS.ajaxRequest(addUrl, {
        parameters: {
          '${PARAM_PROJECT_ID}':'${project.projectId}',
          '${PARAM_RESOURCE_NAME}': resource_name,
          '${PARAM_RESOURCE_QUOTA}': quota
        },
        onSuccess: function() {
          console.log('Resource with quota created successfully!');
        }
      });
    } else {
      alert('Creating resource without quota');
      //noinspection JSDuplicatedDeclaration
      BS.ajaxRequest(addUrl, {
        parameters: {
          '${PARAM_PROJECT_ID}':'${project.projectId}',
          '${PARAM_RESOURCE_NAME}':resource_name
        },
        onSuccess: function() {
          console.log('Resource without quota created successfully!');
        }
      });
    }
  }
});
</script>

<tr>
  <td colspan="2" style="padding:0; margin:0;">
    <table id="locksTaken" class="dark borderBottom">
      <thead>
      <tr>
        <th>Lock Name</th>
        <th style="width: 10%">Lock Type</th>
        <th colspan="2" style="width: 10%">&nbsp;</th>
      </tr>
      </thead>
      <tbody>
      </tbody>
    </table>
    <table id="noLocksTaken" style="display: none">
      <tr>
        <td>No locks are currently added</td>
      </tr>
    </table>
  </td>
</tr>

<script type="text/javascript">
  BS.LocksDialog.fillData();
  BS.LocksDialog.refreshUi();
</script>

<tr class="noBorder" style="display: none">
  <th><label for="${keys.locksFeatureParamKey}">Resource name:</label></th>
  <td>
    <props:multilineProperty name="${keys.locksFeatureParamKey}" linkTitle="Enter shared resource name(s)" cols="49" rows="5" expanded="${false}"/>
    <span class="error" id="error_${keys.locksFeatureParamKey}"></span>
    <span class="smallNote">Please specify shared resources that must be locked during build</span>
  </td>
</tr>

<tr class="noBorder">
  <td colspan="2">
    <forms:addButton id="addNewLock" onclick="BS.LocksDialog.showDialog();   return false">Add lock</forms:addButton>
    <bs:dialog dialogId="locksDialog" title="Lock Managemet" closeCommand="BS.LocksDialog.close()">
      <table class="runnerFormTable">
        <tr>
          <th><label for="lockSource">Resource selection: </label></th>
          <td>
            <forms:select name="lockSource" id="lockSource" style="width: 90%" onchange="BS.LocksDialog.syncResourceSelectionState(); return true;">
              <forms:option value="create">Create a resource to lock</forms:option><%--todo: ui messages--%>
              <forms:option value="choose">Choose a resource to lock</forms:option><%--todo: ui messages--%>
            </forms:select>
            <span class="smallNote">Select, whether you want to create new shared resource or use existing one</span>
          </td>
        </tr>
        <tr id="row_resourceChoose">
          <th><label for="lockFromResources">Resource name:</label></th>
          <td> <%-- todo: here change bean and jstl to javascript--%>
            <c:choose>
              <c:when test="${not empty bean.resources}">
                <forms:select name="lockFromResources" id="lockFromResources" style="width: 90%">
                  <c:forEach items="${bean.resources}" var="resource">
                    <forms:option value="${resource.name}"><c:out value="${resource.name}"/></forms:option>
                  </c:forEach>
                </forms:select>
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
            <forms:textField id="newLockName" name="newLockName" style="width: 98%" maxlength="40" className="longField buildTypeParams" defaultText=""/>
            <span class="smallNote">Enter name of the resource</span>
          </td>
        </tr>
        <tr id="row_useQuotaSwitch">
          <th>Use quota:</th>
          <td>
            <forms:checkbox name="use_quota" id="use_quota" onclick="BS.LocksDialog.toggleUseQuota()" checked="false"/>
          </td>
        </tr>
        <tr id="row_useQuotaInput" style="display: none">
          <th><label for="resource_quota">Resource quota:</label> </th>
          <td><forms:textField name="resource_quota"  style="width: 25%" id='resource_quota' className="longField buildTypeParams" maxlength="3"/></td>
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


