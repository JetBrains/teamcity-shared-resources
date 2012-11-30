package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesController extends BaseController {

  private final ProjectSettingsManager myProjectSettingsManager;

  private final PluginDescriptor myDescriptor;
  private ProjectManager myProjectManager;

  public SharedResourcesController(@NotNull WebControllerManager manager,
                                   @NotNull ProjectSettingsManager projectSettingsManager,
                                   @NotNull PluginDescriptor descriptor,
                                   @NotNull ProjectManager projectManager
  ) {
    myProjectSettingsManager = projectSettingsManager;
    myDescriptor = descriptor;
    myProjectManager = projectManager;

    manager.registerController("/sharedResourcesAdd.html", new BaseController() {
      @Nullable
      @Override
      protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        final String newResource = request.getParameter("new_resource");
        final int quota = Integer.parseInt(request.getParameter("new_resource_quota"));
        final String projectId = request.getParameter("project_id");
        if (newResource != null) {
          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).addResource(Resource.newResource(newResource, quota));
          myProjectManager.findProjectById(projectId).persist();
        }
        return null;
      }
    });

    manager.registerController("/sharedResourcesDelete.html", new BaseController() {
      @Nullable
      @Override
      protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        final String resourceName = request.getParameter("resource_name");
        final String projectId = request.getParameter("project_id");
        if (resourceName != null) {
//          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).addResource(Resource.newResource(newResource, quota));
//          myProjectManager.findProjectById(projectId).persist();
        }
        return null;
      }
    });

  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    return null;
  }
}
