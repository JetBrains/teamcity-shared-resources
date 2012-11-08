package jetbrains.buildServer.sharedResources.pages;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.PositionConstraint;
import jetbrains.buildServer.web.openapi.project.ProjectTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;

/**
 * Created with IntelliJ IDEA.
 * Date: 24.10.12
 * Time: 13:43
 *
 * @author Oleg Rybak
 */
public class SharedResourcesProjectPage extends ProjectTab {

  private static final Logger LOG = Logger.getInstance(SharedResourcesProjectPage.class.getName());

  private ProjectSettingsManager myProjectSettingsManager;

  protected SharedResourcesProjectPage(@NotNull PagePlaces pagePlaces,
                                       @NotNull ProjectManager projectManager,
                                       @NotNull PluginDescriptor descriptor,
                                       @NotNull ProjectSettingsManager projectSettingsManager
  ) {
    super("sharedResources", "Shared Resources", pagePlaces, projectManager, descriptor.getPluginResourcesPath("projectPage.jsp"));
    myProjectSettingsManager = projectSettingsManager;
    setPosition(PositionConstraint.after("problems"));
  }


  // unchecked warnings were suppressed to enable compilation against 7.0 codebase
  @Override
  @SuppressWarnings("unchecked")
  protected void fillModel(@NotNull Map model, @NotNull HttpServletRequest request, @NotNull SProject project, @Nullable SUser user) {
    SharedResourcesProjectSettings settings = (SharedResourcesProjectSettings) myProjectSettingsManager.getSettings(project.getProjectId(), SERVICE_NAME);
    if (isPost(request)) {
      final String newResource = request.getParameter("new_resource");
      if (newResource != null && !"".equals(newResource)) {
        settings.addResource(newResource);
        project.persist();
      }
    }

    SharedResourcesBean bean = new SharedResourcesBean(settings.getSharedResourceNames());
    model.put("bean", bean);
  }
}
