package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.controllers.buildType.ParameterInfo;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.pages.SharedResourcesBean;
import jetbrains.buildServer.sharedResources.settings.SharedResourcesProjectSettings;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.*;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesBuildFeatureController extends BaseController {

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final EditBuildTypeFormFactory myFormFactory;

  @NotNull
  private final ProjectSettingsManager myProjectSettingsManager;

  public SharedResourcesBuildFeatureController(
          @NotNull final PluginDescriptor descriptor,
          @NotNull final WebControllerManager web,
          @NotNull final EditBuildTypeFormFactory formFactory,
          @NotNull final ProjectSettingsManager projectSettingsManager) {
    myDescriptor = descriptor;
    myFormFactory = formFactory;
    myProjectSettingsManager = projectSettingsManager;
    web.registerController(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_HTML), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request,
                                  @NotNull HttpServletResponse response) throws Exception {

    final ModelAndView result = new ModelAndView(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_JSP));
    final EditableBuildTypeSettingsForm form = myFormFactory.getOrCreateForm(request);
    final List<ParameterInfo> configParams = form.getBuildTypeParameters().getConfigurationParameters();
    final SProject project = form.getProject();

    // used resources
    final List<String> readLockNames = new ArrayList<String>();
    final List<String> writeLockNames = new ArrayList<String>();

    // available resources
    final Set<String> availableResources = new LinkedHashSet<String>();

    final SharedResourcesProjectSettings settings = (SharedResourcesProjectSettings)myProjectSettingsManager.getSettings(project.getProjectId(), SharedResourcesPluginConstants.SERVICE_NAME);
    availableResources.addAll(settings.getSharedResourceNames());

    for (ParameterInfo param : configParams) {
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(param.getName());
      if (lock != null) {
        switch (lock.getType()) {
          case READ:
            readLockNames.add(lock.getName());
            availableResources.remove(lock.getName());
            break;
          case WRITE:
            writeLockNames.add(lock.getName());
            availableResources.remove(lock.getName());
            break;
        }
      }
    }
    final Map<String, Object> model = result.getModel();
    model.put(ATTR_READ_LOCKS, readLockNames);
    model.put(ATTR_WRITE_LOCKS, writeLockNames);

    final SharedResourcesBean bean = new SharedResourcesBean(availableResources);
    model.put("bean", bean);

    /*BuildTypeSettings _settings = form.getSettings();
    _settings.addParameter(
            new SimpleParameter("teamcity.locks.readLock.hello", "")

    );
    form.persistSettings();*/
    return result;
  }
}
/*
 * 1) How do we add parameter to build configuration?       BuildTypeSettings#addParameter
 * 2) How do we remove parameter from build configuration?  BuildTypeSettings#removeBuildParameter
 * 3) What is the correct way to deal with parameters?
 */
