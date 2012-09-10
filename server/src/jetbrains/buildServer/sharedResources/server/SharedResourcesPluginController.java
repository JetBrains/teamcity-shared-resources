package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.BuildFeatureBean;
import jetbrains.buildServer.controllers.admin.projects.BuildFeaturesBean;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.util.FeatureUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.*;

/**
 *
 * @author Oleg Rybak
 */
public class SharedResourcesPluginController extends BaseController {

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final SharedResourcesFeatureContext myContext;

  @NotNull
  private final EditBuildTypeFormFactory myFormFactory;

  public SharedResourcesPluginController(
          @NotNull PluginDescriptor descriptor,
          @NotNull SharedResourcesFeatureContext context,
          @NotNull WebControllerManager web,
          @NotNull EditBuildTypeFormFactory formFactory) {
    myDescriptor = descriptor;
    myContext = context;
    myFormFactory = formFactory;
    web.registerController(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_HTML), this);

  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request,
                                  @NotNull HttpServletResponse response) throws Exception {
    final ModelAndView result = new ModelAndView(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_JSP));
    final EditableBuildTypeSettingsForm form = myFormFactory.getOrCreateForm(request);
    final BuildFeaturesBean bean = form.getBuildFeaturesBean();
    final Set<String> otherResourceNames = new HashSet<String>();
    final Map<String, String> properties = bean.getPropertiesBean().getProperties();
    final SBuildType buildType = form.getSettingsBuildType();

    if (buildType != null) {
      // let feature know, to what configuration it belongs
      final String buildTypeId = buildType.getBuildTypeId();
      request.setAttribute(SharedResourcesPluginConstants.BUILD_ID_KEY, buildType.getBuildTypeId());
      final String myResourceName = properties.get(RESOURCE_PARAM_KEY);
      final Collection<BuildFeatureBean> beans = bean.getBuildFeatureDescriptors();
      for (BuildFeatureBean b: beans) {
        FeatureUtil.extractResource(otherResourceNames, b.getDescriptor());
      }

      otherResourceNames.remove(myResourceName);
      myContext.putNamesInContext(buildTypeId, otherResourceNames);
    }
    return result;
  }
}
