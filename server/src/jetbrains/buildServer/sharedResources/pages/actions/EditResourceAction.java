package jetbrains.buildServer.sharedResources.pages.actions;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("UnusedShould")
public final class EditResourceAction extends BaseResourceAction implements ControllerAction {

  private SharedResourcesFeatures myFeatures;

  public EditResourceAction(@NotNull final ProjectManager projectManager,
                            @NotNull final Resources resources,
                            @NotNull final SharedResourcesFeatures features) {
    super(projectManager, resources);
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

    final String oldResourceName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME);
    final String projectId = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID);
    final SProject project = myProjectManager.findProjectById(projectId);

    if (project != null) {
      final Resource resource = getResourceFromRequest(request);
      if (resource != null) {
        final String newName = resource.getName();
        final boolean nameChanged = !resource.getName().equals(oldResourceName);
        if (nameChanged) {
          // check that it is not used by any resource in any project
          if (resourceExists(newName)) {
            createNameError(ajaxResponse);
            return;
          } else {
            // my resource can be used only in my build configurations or in build configurations in my subtree
            final List<SProject> myAllSubprojects = project.getAllSubProjects();
            myAllSubprojects.add(project);
            for (SProject p : myAllSubprojects) {
              final List<SBuildType> buildTypes = p.getBuildTypes();
              for (SBuildType type : buildTypes) {
                // todo: do we need resolved features here? Using unresolved for now
                for (SharedResourcesFeature feature : myFeatures.searchForFeatures(type)) {
                  feature.updateLock(type, oldResourceName, newName);
                }
              }
              p.persist();
            }
          }
        }
        myResources.editResource(projectId, oldResourceName, resource);
        project.persist();
        // here must check for existence
      }
    }
  }
}
