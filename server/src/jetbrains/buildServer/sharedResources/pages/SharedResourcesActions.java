/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.pages;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.SharedResourcesUtils;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.LOCKS_FEATURE_PARAM_KEY;
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
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        Resource resource = getResourceFromRequest(request);
        if (resource != null) {
          ((SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME)).editResource(oldResourceName, resource);
          if (!resource.getName().equals(oldResourceName)) {
            // name was changed. update references
            final List<SBuildType> buildTypes = project.getBuildTypes();
            for (SBuildType type: buildTypes) {
              final SBuildFeatureDescriptor descriptor = SharedResourcesUtils.searchForFeature(type, false);
              if (descriptor != null) {
                // we have feature. now:
                // 1) get locks
                final Map<String, String> parameters = descriptor.getParameters();
                final String locksString = parameters.get(SharedResourcesPluginConstants.LOCKS_FEATURE_PARAM_KEY);
                final Map<String, Lock> lockMap = SharedResourcesUtils.getLocksMap(locksString);
                // 2) search for lock with old resource name
                final Lock lock = lockMap.get(oldResourceName);
                if (lock != null) {
                  // 3) save its type
                  final LockType lockType = lock.getType();
                  // 4) remove it
                  lockMap.remove(oldResourceName);
                  // 5) add lock with new resource name and saved type
                  lockMap.put(resource.getName(), new Lock(resource.getName(), lockType));
                  // 6) serialize locks
                  final String locksAsString = SharedResourcesUtils.locksAsString(lockMap.values());
                  // 7) update build feature parameters
                  Map<String, String> newParams = new HashMap<String, String>(parameters);
                  newParams.put(LOCKS_FEATURE_PARAM_KEY, locksAsString);
                  // 8) update build feature
                  type.updateBuildFeature(descriptor.getId(), descriptor.getType(), newParams);
                }
              }
            }
          }
          project.persist();
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
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
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
