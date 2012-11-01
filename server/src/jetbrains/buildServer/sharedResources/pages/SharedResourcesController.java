package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
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

  private ProjectSettingsManager myProjectSettingsManager;
  private PluginDescriptor myDescriptor;

  public SharedResourcesController(@NotNull SBuildServer server,
                                   @NotNull WebControllerManager manager,
                                   @NotNull ProjectSettingsManager projectSettingsManager,
                                   @NotNull PluginDescriptor descriptor
  ) {
    myProjectSettingsManager = projectSettingsManager;
    myDescriptor = descriptor;
    manager.registerController("/sharedResourcesDelete.html", new BaseController() {
      @Nullable
      @Override
      protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        final String resourceToDelete = request.getParameter("delete");
        final String projectId = request.getParameter("project_id");
        if (resourceToDelete != null) {
          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).remove(resourceToDelete);
        }
        return null;
      }
    });

    manager.registerController("/sharedResourcesAdd.html", new BaseController() {
      @Nullable
      @Override
      protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
        final String newResource = request.getParameter("new_resource");
        final String projectId = request.getParameter("project_id");
        if (newResource != null) {
          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).addResource(newResource);
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
