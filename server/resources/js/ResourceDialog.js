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
  myData: {},

  getContainer: function () {
    return $('resourcesFormDialog');
  },

  formElement: function() {
    return $('resourcesForm');
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
    $j('#resource_id').val('');
    this.showCommon();
  },

  showEdit: function (resource_id) {
    this.editMode = true;
    this.id = resource_id;
    this.clearErrors();
    $j('#resource_id').val(resource_id);
    var r = this.myData[resource_id]; // current resource contents
    this.currentResourceName = r['name'];
    $j('#resource_name').val(this.currentResourceName);
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

    $j('#resource_enabled').prop('checked', r['enabled']);

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
    Form.enable(this.formElement());
    this.setSaving(false);
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
    $j('#resource_name').focus();
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
    Form.disable(this.formElement());
    this.setSaving(true);
    if (this.editMode) {
      BS.SharedResourcesActions.editResource(this.id, this.currentResourceName);
    } else {
      BS.SharedResourcesActions.addResource();
    }
    return false;
  },

  afterSubmit: function(errors) {
    this.setSaving(false);
    Form.enable(this.formElement());
    if (!errors) {
      this.close();
      window.location.reload();
    } else {
      return false;
    }
  },

  setSaving: function(saving) {
    if (saving) {
      BS.Util.show('resourceDialogSaving');
    } else {
      BS.Util.hide('resourceDialogSaving');
    }
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
      var val = $j.trim($j('#customValues').val());
      if (val === '') {
        BS.Util.show('error_Values');
        $j('#error_Values').html("Please define custom values for resource");
        errorsPresent = true;
      }
    } else if (flag === 'quoted') {
      var val = $j.trim($j('#resource_quota').val());
      if (val.length === 0) {
        BS.Util.show('error_Quota');
        $j('#error_Quota').html("Value must not be empty");
        errorsPresent = true;
      }
      if (!/^[0-9]+$/.test(val)) {
        BS.Util.show('error_Quota');
        var message = "Value " + val + " is not correct";
        $j('#error_Quota').html(message.escapeHTML());
        errorsPresent = true;
      }
    }

    var element = $j('#resource_name');
    var value = $j.trim(element.val());
    if (value.length === 0) { // check not empty
      BS.Util.show('error_Name');
      $j('#error_Name').html("Name must not be empty");
      errorsPresent = true;
    } else if (!/^[A-Za-z0-9_]+$/.test(value)) {
      BS.Util.show('error_Name');
      $j('#error_Name').html("The name is invalid. It should start with a latin letter and contain only latin letters, digits and underscores (80 characters max).");
      errorsPresent = true;
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
