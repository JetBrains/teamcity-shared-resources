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
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.WEB;

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
                                @NotNull final ProjectManager projectManager,
                                @NotNull final SharedResourcesFeatures features,
                                @NotNull final Resources resources) {
    manager.registerController(WEB.ACTION_ADD, new AddController(projectManager, resources));
    manager.registerController(WEB.ACTION_EDIT, new EditController(projectManager, features, resources));
    manager.registerController(WEB.ACTION_DELETE, new DeleteController(projectManager, resources));
  }

  static final class AddController extends BaseSimpleController {

    public AddController(@NotNull final ProjectManager projectManager,
                         @NotNull final Resources resources) {
      super(projectManager, resources);
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
          myResources.addResource(projectId, resource);
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
    private final SharedResourcesFeatures myFeatures;

    public EditController(@NotNull final ProjectManager projectManager,
                          @NotNull final SharedResourcesFeatures features,
                          @NotNull final Resources resources) {
      super(projectManager, resources);
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
        final Resource resource = getResourceFromRequest(request);
        if (resource != null) {
          myResources.editResource(projectId, oldResourceName, resource);
          if (!resource.getName().equals(oldResourceName)) {
            // name was changed. update references
            final List<SBuildType> buildTypes = project.getBuildTypes();
            for (SBuildType type: buildTypes) {
              // todo: do we need resolved features here? Using unresolved for now
              for (SharedResourcesFeature feature: myFeatures.searchForFeatures(type)) {
                feature.updateLock(type, oldResourceName, resource.getName());
              }
            }
            project.persist();
          }
        }
      }
      return null;
    }
  }

  static final class DeleteController extends BaseSimpleController {

    public DeleteController(@NotNull final ProjectManager projectManager,
                            @NotNull final Resources resources) {
      super(projectManager, resources);
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response) throws Exception {
      final String resourceName = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final String projectId = request.getParameter(WEB.PARAM_PROJECT_ID);
      final SProject project = myProjectManager.findProjectById(projectId);
      if (project != null) {
        myResources.deleteResource(projectId, resourceName); // todo: maybe change to project
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
    protected final ProjectManager myProjectManager;

    @NotNull
    protected final Resources myResources;

    public BaseSimpleController(@NotNull final ProjectManager projectManager,
                                @NotNull final Resources resources) {
      myProjectManager = projectManager;
      myResources = resources;
    }

    @Nullable
    protected static Resource getResourceFromRequest(@NotNull final HttpServletRequest request) {
      final String resourceName = request.getParameter(WEB.PARAM_RESOURCE_NAME);
      final ResourceType resourceType = ResourceType.fromString(request.getParameter(WEB.PARAM_RESOURCE_TYPE));
      Resource resource = null;
      if (ResourceType.QUOTED.equals(resourceType)) {
        final String resourceQuota = request.getParameter(WEB.PARAM_RESOURCE_QUOTA);
        if (resourceQuota != null && !"".equals(resourceQuota)) { // we have quoted resource
          try {
            int quota = Integer.parseInt(resourceQuota);
            resource = ResourceFactory.newQuotedResource(resourceName, quota);
          } catch (IllegalArgumentException e) {
            LOG.warn("Illegal argument supplied in quota for resource [" + resourceName + "]");
          }
        } else { // we have infinite resource
          resource = ResourceFactory.newInfiniteResource(resourceName);
        }
      } else if (ResourceType.CUSTOM.equals(resourceType)) {
        final String values = request.getParameter(WEB.PARAM_RESOURCE_VALUES);
        final Collection<String> strings = StringUtil.split(values, true, '\r', '\n');
        resource = ResourceFactory.newCustomResource(resourceName, strings);
      }
      return resource;
    }
  }

}
