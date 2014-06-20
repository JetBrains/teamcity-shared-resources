package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Class {@code AddResourceAction}
 *
 * Defines action for adding new resources
 *
 * @see BaseResourceAction
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class AddResourceAction extends BaseResourceAction implements ControllerAction {

  @NotNull
  @Override
  public String getActionName() {
    return "addResource";
  }

  public AddResourceAction(@NotNull final ProjectManager projectManager,
                           @NotNull final Resources resources,
                           @NotNull final ResourceHelper resourceHelper,
                           @NotNull final Messages messages) {
    super(projectManager, resources, resourceHelper, messages);
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      final Resource resource = myResourceHelper.getResourceFromRequest(projectId, request);
      if (resource != null) {
        try {
          myResources.addResource(resource);
          project.persist();
          addMessage(request, "Resource " + resource.getName() + " was added");
        } catch (DuplicateResourceException e) {
          createNameError(ajaxResponse, resource.getName());
        }
      }
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
