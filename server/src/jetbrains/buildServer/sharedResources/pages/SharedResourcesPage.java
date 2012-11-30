package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.controllers.admin.projects.EditProjectTab;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesPage extends EditProjectTab {

  private ProjectSettingsManager myProjectSettingsManager;

  public SharedResourcesPage(@NotNull PagePlaces pagePlaces, @NotNull ProjectManager projectManager, @NotNull PluginDescriptor descriptor, @NotNull ProjectSettingsManager projectSettingsManager) {
    super(pagePlaces, SharedResourcesPluginConstants.PLUGIN_NAME, descriptor.getPluginResourcesPath("projectPage.jsp"), "Shared Resources", projectManager);
    myProjectSettingsManager = projectSettingsManager;
  }

  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    super.fillModel(model, request);
    SharedResourcesBean bean;
    final String projectId = request.getParameter("projectId");
    if (projectId != null) {
      SharedResourcesProjectSettings settings = (SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(projectId, SERVICE_NAME);
      if (request.getParameter("add") != null) { // todo: remove sample data
        settings.putSampleData();
        getProject(request).persist();
      }
      bean = new SharedResourcesBean(settings.getResources());
    } else {
      bean = new SharedResourcesBean(Collections.<Resource>emptyList()); // todo: how to differentiate error vs no resources??!
    }
    model.put("bean", bean);
  }
}
