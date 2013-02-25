package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("UnusedShould")
public final class AddResourceAction extends BaseResourceAction implements ControllerAction {

  @NotNull
  @Override
  public String getActionName() {
    return "addResource";
  }

  public AddResourceAction(@NotNull final ProjectManager projectManager, @NotNull final Resources resources) {
    super(projectManager, resources);
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      Resource resource = getResourceFromRequest(request);
      if (resource != null) {
        if (resourceExists(resource.getName())) {
          createNameError(ajaxResponse);
          return;
        }
        myResources.addResource(projectId, resource);
        project.persist();
      }
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
