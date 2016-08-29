package jetbrains.buildServer.sharedResources.pages.actions;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

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
                           @NotNull final ResourceProjectFeatures projectFeatures,
                           @NotNull final ResourceHelper resourceHelper,
                           @NotNull final Messages messages,
                           @NotNull final ConfigActionFactory configActionFactory) {
    super(projectManager, projectFeatures, resourceHelper, messages, configActionFactory);
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);
    if (project != null) {
      final Map<String, String> resourceParameters = myResourceHelper.getNewResourceFromRequest(request);
      if (resourceParameters != null) {
        myProjectFeatures.addFeature(project, resourceParameters);
        project.persist(myConfigActionFactory.createAction(project, "'" + resourceParameters.get(SharedResourcesPluginConstants.ProjectFeatureParameters.NAME) + "' shared resource was created"));
        addMessage(request, "Resource " + resourceParameters.get(SharedResourcesPluginConstants.ProjectFeatureParameters.NAME) + " was added");
      } else {
        LOG.error("Failed to create new resource"); // todo: proper logging
      }
    } else {
      LOG.error("Project [" + projectId + "] no longer exists!");
    }
  }
}
