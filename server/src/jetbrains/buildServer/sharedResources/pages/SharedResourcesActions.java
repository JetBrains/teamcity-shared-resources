/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.SharedResourcesUtils;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourceFeatures;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.WEB;
import static jetbrains.buildServer.sharedResources.server.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Class {@code SharedResourcesActions}
 *
 * Contains controller definitions for resource management
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesActions {

  private static final Logger LOG = Logger.getInstance(SharedResourcesActions.class.getName());

  public SharedResourcesActions(@NotNull final WebControllerManager manager,
                                @NotNull final ProjectSettingsManager projectSettingsManager,
                                @NotNull final ProjectManager projectManager,
                                @NotNull final SharedResourceFeatures features) {
    manager.registerController(WEB.ACTION_ADD, new AddController(projectSettingsManager, projectManager));
    manager.registerController(WEB.ACTION_EDIT, new EditController(projectSettingsManager, projectManager, features));
    manager.registerController(WEB.ACTION_DELETE, new DeleteController(projectSettingsManager, projectManager));
  }

  static final class AddController extends BaseSimpleController {

    public AddController(@NotNull final ProjectSettingsManager projectSettingsManager,
                         @NotNull final ProjectManager projectManager) {
      super(projectManager, projectSettingsManager);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response) throws Exception {
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        Resource resource = getResourceFromRequest(request);
        if (resource != null) {
          ((PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME)).addResource(resource);
          project.persist();
        }
      } else {
        LOG.error("Project [" + projectId + "] no longer exists!" );
      }
      return null;
    }
  }

  static final class EditController extends BaseSimpleController {

    @NotNull
    private final SharedResourceFeatures myFeatures;

    public EditController(@NotNull final ProjectSettingsManager projectSettingsManager,
                          @NotNull final ProjectManager projectManager,
                          @NotNull final SharedResourceFeatures features) {
      super(projectManager, projectSettingsManager);
      myFeatures = features;
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response) throws Exception {
      final String oldResourceName = request.getParameter(WEB.PARAM_OLD_RESOURCE_NAME);
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        Resource resource = getResourceFromRequest(request);
        if (resource != null) {
          ((PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME)).editResource(oldResourceName, resource);
          if (!resource.getName().equals(oldResourceName)) {
            // name was changed. update references
            final List<SBuildType> buildTypes = project.getBuildTypes();
            for (SBuildType type: buildTypes) {
              final Collection<SBuildFeatureDescriptor> descriptors = myFeatures.searchForFeatures(type);
              for (SBuildFeatureDescriptor descriptor: descriptors) {
                // we have feature. now:
                // 1) get locks
                final Map<String, String> parameters = descriptor.getParameters();
                final String locksString = parameters.get(LOCKS_FEATURE_PARAM_KEY);
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

    public DeleteController(@NotNull final ProjectSettingsManager projectSettingsManager,
                            @NotNull final ProjectManager projectManager) {
      super(projectManager, projectSettingsManager);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response) throws Exception {
      final String resourceName = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        ((PluginProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME)).deleteResource(resourceName);
        project.persist();
        // todo: it should not be allowed to delete resource, that is in use
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

    public BaseSimpleController(@NotNull final ProjectManager projectManager,
                                @NotNull final ProjectSettingsManager projectSettingsManager) {
      myProjectManager = projectManager;
      myProjectSettingsManager = projectSettingsManager;
    }

    @Nullable
    protected static Resource getResourceFromRequest(@NotNull final HttpServletRequest request) {
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
