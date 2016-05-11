package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class {@code DeleteResourceAction}
 *
 * Defines action for deleting resources
 *
 * @see BaseResourceAction
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class DeleteResourceAction extends BaseResourceAction implements ControllerAction {

  public DeleteResourceAction(@NotNull final ProjectManager projectManager,
                              @NotNull final Resources resources,
                              @NotNull final ResourceHelper resourceHelper,
                              @NotNull final Messages messages,
                              @NotNull final ConfigActionFactory configActionFactory,
                              @NotNull final ResourceProjectFeatures resourceProjectFeatures) {
    super(projectManager, resources, resourceHelper, messages, configActionFactory, resourceProjectFeatures);
  }

  @NotNull
  @Override
  public String getActionName() {
    return "deleteResource";
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String resourceName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      myResources.deleteResource(projectId, resourceName);
      myResourceProjectFeatures.deleteResource(project, resourceName);
      project.persist(myConfigActionFactory.createAction(project, "'" + resourceName + "' shared resource was removed"));
      addMessage(request, "Resource " + resourceName + " was deleted");
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
