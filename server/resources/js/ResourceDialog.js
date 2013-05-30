/**
 * Created with IntelliJ IDEA.
 * User: Oleg.Rybak
 * Date: 05.02.13
 * Time: 15:19
 * To change this template use File | Settings | File Templates.
 */
BS.ResourceDialog = OO.extend(BS.AbstractModalDialog, {
  attachedToRoot: false,
  editMode: false,
  currentResourceName: "",
  existingResources: {},
  myData: {},
  getContainer: function () {
    return $('resourceDialog');
  },

  showDialog: function () {
    this.editMode = false;
    this.clearErrors();

    $j('#resource_type option').each(function () {
      var self = $j(this);
      self.prop("selected", self.val() == 'infinite');
    });
    $j('#resource_quota').val(1);
    $j('#customValues').val('');
    $j('#resource_name').val('');
    this.showCommon();
  },

  showEdit: function (resource_name) {
    this.editMode = true;
    this.clearErrors();
    this.currentResourceName = resource_name;
    $j('#resource_name').val(resource_name);
    var r = this.myData[resource_name]; // current resource contents
    var type = r['type'];
    if (r['infinite']) {
      type = "infinite"
    } else {
      type = type.toLowerCase();
    }
    $j('#resource_type option').each(function () {
      var self = $j(this);
      self.prop("selected", self.val() == type);
    });

    if (type === 'quoted') {
      $j('#resource_quota').val(r['quota']);
    } else if (type === 'custom') {
      $j('#resource_quota').val(1);
      $j('#customValues').val(r['customValues'].join('\n'));
    } else {
      $j('#resource_quota').val(1);
    }
    this.showCommon();
  },

  showCommon: function() {
    this.adjustDialogDisplay(this.editMode);
    this.showCentered();
    this.bindCtrlEnterHandler(this.submit.bind(this));
    BS.Util.hide('info_row');
    if (this.editMode) {
      $j('#resource_name').bind('input propertychange', this.onNameChange);
    } else {
      $j('#resource_name').unbind('input propertychange', this.onNameChange);
    }
    BS.MultilineProperties.updateVisible();
  },

  adjustDialogDisplay: function (editMode) {
    if (editMode) {
      $j("#resourceDialogTitle").html('Edit Resource');
    } else {
      $j("#resourceDialogTitle").html('Add Resource');
    }
    this.syncResourceSelectionState();
  },

  submit: function () {
    if (!this.validate()) return false;
    var result = true;
    if (this.editMode) {
      result = BS.SharedResourcesActions.editResource(this.currentResourceName);
    } else {
      result = BS.SharedResourcesActions.addResource();
    }
    if (result) {
      this.close();
    }
    return false;
  },

  clearErrors: function () {
    BS.Util.hide('error_Name');
    $j('#error_Name').html("");
    BS.Util.hide('error_Quota');
    $j('#error_Quota').html("");
    BS.Util.hide('error_Values');
    $j('#error_Values').html("");
  },

  validate: function () {
    var errorsPresent = false;
    this.clearErrors();
    var flag = $j('#resource_type option:selected').val();
    if (flag === 'custom') {
      var val = $j('#customValues').val().trim();
      if (val === '') {
        BS.Util.show('error_Values');
        $j('#error_Values').html("Please define custom values for resource");
        errorsPresent = true;
      }
    }

    var element = $j('#resource_name');
    var value = element.val().trim();
    if (value.length === 0) { // check not empty
      BS.Util.show('error_Name');
      $j('#error_Name').html("Name must not be empty");
      errorsPresent = true;
    }
    if ((this.editMode && (this.currentResourceName !== value)) || (!this.editMode)) {
      // name changed
      if (this.existingResources[value]) {
        // quick check for current subtree
        BS.Util.show('error_Name');
        $j('#error_Name').html("Name is already used");
        errorsPresent = true;
      }
    }
    element.val(value);
    return !errorsPresent;
  },

  syncResourceSelectionState: function () {
    var flag = $j('#resource_type option:selected').val();
    if (flag === 'infinite') {
      this.toggleModeInfinite();
    } else if (flag === 'quoted') {
      this.toggleModeQuota();
    } else if (flag === 'custom') {
      this.toggleModeCustom();
    }
    BS.MultilineProperties.updateVisible();
  },

  toggleModeInfinite: function () {
    BS.Util.hide('quota_row');
    BS.Util.hide('custom_row');
  },

  toggleModeQuota: function () {
    BS.Util.show('quota_row');
    BS.Util.hide('custom_row');
  },

  toggleModeCustom: function () {
    BS.Util.hide('quota_row');
    BS.Util.show('custom_row');
  },

  onNameChange: function() {
    if (BS.ResourceDialog.currentResourceName !== $j('#resource_name').val()) {
      BS.Util.show('nameAttention');
    } else {
      BS.Util.hide('nameAttention');
    }
  }
});
