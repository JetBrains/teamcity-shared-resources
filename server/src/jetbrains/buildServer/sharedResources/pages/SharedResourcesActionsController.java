

package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.BaseAjaxActionController;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.pages.actions.AddResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.DeleteResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.EditResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.EnableDisableResourceAction;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesActionsController extends BaseAjaxActionController {

  public SharedResourcesActionsController(@NotNull final WebControllerManager controllerManager,
                                          @NotNull final AddResourceAction addResourceAction,
                                          @NotNull final DeleteResourceAction deleteResourceAction,
                                          @NotNull final EditResourceAction editResourceAction,
                                          @NotNull final EnableDisableResourceAction enableDisableResourceAction) {
    super(controllerManager);
    controllerManager.registerController(SharedResourcesPluginConstants.WEB.ACTIONS, this);
    registerAction(addResourceAction);
    registerAction(deleteResourceAction);
    registerAction(editResourceAction);
    registerAction(enableDisableResourceAction);
  }
}