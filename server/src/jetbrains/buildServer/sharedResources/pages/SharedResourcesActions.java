package jetbrains.buildServer.sharedResources.pages;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.WEB;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesActions {

  private static final Logger LOG = Logger.getInstance(SharedResourcesActions.class.getName());

  public SharedResourcesActions(@NotNull WebControllerManager manager,
                                @NotNull ProjectSettingsManager projectSettingsManager,
                                @NotNull ProjectManager projectManager
  ) {
    manager.registerController("/sharedResourcesAdd.html", new AddController(projectSettingsManager, projectManager));
    manager.registerController("/sharedResourcesEdit.html", new EditController(projectSettingsManager, projectManager));
    manager.registerController("/sharedResourcesDelete.html", new DeleteController(projectSettingsManager, projectManager));
  }

  static final class AddController extends BaseSimpleController {

    public AddController(@NotNull ProjectSettingsManager projectSettingsManager,
                         @NotNull ProjectManager projectManager) {
      super(projectManager, projectSettingsManager);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        Resource resource = getResourceFromRequest(request);
        if (resource != null) {
          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).addResource(resource);
          project.persist();
        }
      } else {
        LOG.error("Project [" + projectId + "] no longer exists!" );
      }
      return null;
    }
  }

  static final class EditController extends BaseSimpleController {

    public EditController(@NotNull ProjectSettingsManager projectSettingsManager,
                          @NotNull ProjectManager projectManager) {
      super(projectManager, projectSettingsManager);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
      final String oldResourceName = request.getParameter(WEB.PARAM_OLD_RESOURCE_NAME);
      final String projectId = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        Resource resource = getResourceFromRequest(request);
        if (resource != null) {
          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).editResource(oldResourceName, resource);
          project.persist();
          // go through all build configurations, switch name of the resource
        }
      }
      return null;
    }
  }

  static final class DeleteController extends BaseSimpleController {


    public DeleteController(@NotNull ProjectSettingsManager projectSettingsManager, @NotNull ProjectManager projectManager) {
      super(projectManager, projectSettingsManager);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
      final String resourceName = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final String projectId = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).deleteResource(resourceName);
        project.persist();
        // it should not be allowed to delete resource, that is in use
      } else {
        LOG.error("Project [" + projectId + "] no longer exists!" );
      }
      return null;
    }
  }

  static abstract class BaseSimpleController extends BaseController {
    @NotNull
    protected final ProjectSettingsManager myProjectSettingsManager;

    @NotNull
    protected final ProjectManager myProjectManager;

    public BaseSimpleController(@NotNull ProjectManager projectManager, @NotNull ProjectSettingsManager projectSettingsManager) {
      myProjectManager = projectManager;
      myProjectSettingsManager = projectSettingsManager;
    }

    static Resource getResourceFromRequest(HttpServletRequest request) {
      final String resourceName = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final String resourceQuota = request.getParameter(WEB.PARAM_RESOURCE_QUOTA);
      Resource resource = null;
      if (resourceQuota != null && !"".equals(resourceQuota)) { // we have quoted resource
        try {
          int quota = Integer.parseInt(resourceQuota);
          resource = Resource.newResource(resourceName, quota);
        } catch (IllegalArgumentException e) {
          LOG.warn("Illegal argument supplied in quota for resource [" + resourceName + "]");
        }
      } else { // we have infinite resource
        resource = Resource.newInfiniteResource(resourceName);
      }
      return resource;
    }
  }

}
