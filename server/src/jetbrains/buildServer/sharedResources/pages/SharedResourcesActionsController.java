package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.BaseAjaxActionController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.pages.actions.AddResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.DeleteResourceAction;
import jetbrains.buildServer.sharedResources.pages.actions.EditResourceAction;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesActionsController extends BaseAjaxActionController {

  public SharedResourcesActionsController(@NotNull final WebControllerManager controllerManager,
                                          @NotNull final ProjectManager projectManager,
                                          @NotNull final Resources resources,
                                          @NotNull final SharedResourcesFeatures features) {
    super(controllerManager);
    controllerManager.registerController(SharedResourcesPluginConstants.WEB.ACTIONS, this);
    registerAction(new AddResourceAction(projectManager, resources));
    registerAction(new DeleteResourceAction(projectManager, resources));
    registerAction(new EditResourceAction(projectManager, resources, features));
  }
}
