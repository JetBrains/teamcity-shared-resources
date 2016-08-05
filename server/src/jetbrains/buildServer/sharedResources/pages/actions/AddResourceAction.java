package jetbrains.buildServer.sharedResources.pages.actions;

import java.util.Map;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
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
                           @NotNull final Messages messages,
                           @NotNull final ConfigActionFactory configActionFactory) {
    super(projectManager, resources, resourceHelper, messages, configActionFactory);
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      final Map<String, String> newResource = myResourceHelper.getNewResourceFromRequest(request);
      if (newResource != null) {
        myResources.addResource(project, newResource);
        project.persist(myConfigActionFactory.createAction(project, "'" + newResource.get(SharedResourcesPluginConstants.ProjectFeatureParameters.NAME) + "' shared resource was created"));
        addMessage(request, "Resource " + newResource.get(SharedResourcesPluginConstants.ProjectFeatureParameters.NAME) + " was added");
      } else {
        LOG.error("Failed to create new resource"); // todo: proper logging
      }
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
