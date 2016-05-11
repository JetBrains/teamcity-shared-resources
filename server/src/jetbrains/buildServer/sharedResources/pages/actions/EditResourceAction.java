package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Class {@code EditResourceAction}
 *
 * Defines action for editing resources. Supports changing name and type
 * as well as updating all build configurations that use resource being edited
 *
 * @see BaseResourceAction
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class EditResourceAction extends BaseResourceAction implements ControllerAction {

  private SharedResourcesFeatures myFeatures;

  public EditResourceAction(@NotNull final ProjectManager projectManager,
                            @NotNull final Resources resources,
                            @NotNull final ResourceHelper resourceHelper,
                            @NotNull final SharedResourcesFeatures features,
                            @NotNull final Messages messages,
                            @NotNull final ConfigActionFactory configActionFactory,
                            @NotNull final ResourceProjectFeatures resourceProjectFeatures) {
    super(projectManager, resources, resourceHelper, messages, configActionFactory, resourceProjectFeatures);
    myFeatures = features;
  }

  @NotNull
  @Override
  public String getActionName() {
    return "editResource";
  }

  @Override
  protected void doProcess(@NotNull final HttpServletRequest request,
                           @NotNull final HttpServletResponse response,
                           @NotNull final Element ajaxResponse) {

    final String oldName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);

    if (project != null) {
      final Resource resource = myResourceHelper.getResourceFromRequest(projectId, request);
      if (resource != null) {
        final String newName = resource.getName();
        try {
          myResources.editResource(projectId, oldName, resource);
          myResourceProjectFeatures.editResource(project, oldName, myResourceHelper.getResourceParameters(request));
          ConfigAction cause = myConfigActionFactory.createAction(project, "'" + resource.getName() + "' shared resource was updated");
          if (!newName.equals(oldName)) {
            // my resource can be used only in my build configurations or in build configurations in my subtree
            final List<SProject> projects = project.getProjects();
            projects.add(project);
            for (SProject p: projects) {
              boolean updated = false;
              final List<SBuildType> buildTypes = p.getOwnBuildTypes();
              for (SBuildType type: buildTypes) {
                // todo: do we need resolved features here? Using unresolved for now
                for (SharedResourcesFeature feature: myFeatures.searchForFeatures(type)) {
                  updated = updated || feature.updateLock(type, oldName, newName);
                }
              }
              if (updated) {
                p.persist(cause);
              }
            }
          } else { // just persist project so that settings will be saved
            project.persist(cause);
          }
          addMessage(request, "Resource " + newName + " was updated");
        } catch (DuplicateResourceException e) {
          createNameError(ajaxResponse, newName);
        }
      }
    }
  }
}
