package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.BuildFeaturesBean;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.*;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginController extends BaseController {

  @NotNull
  private final PluginDescriptor myDescriptor;

  @NotNull
  private final EditBuildTypeFormFactory myFormFactory;

  public SharedResourcesPluginController(
          @NotNull PluginDescriptor descriptor,
          @NotNull WebControllerManager web,
          @NotNull EditBuildTypeFormFactory formFactory
  ) {
    myDescriptor = descriptor;
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
    final String myLocksString = bean.getPropertiesBean().getProperties().get(LOCKS_FEATURE_PARAM_KEY);
    final Map<String, String> locks = SharedResourcesUtils.splitFeatureParam(myLocksString);
    result.getModel().put("locks", locks);
    return result;
  }


  /*
   *

    final Set<String> otherResourceNames = new HashSet<String>();

    final Collection<BuildFeatureBean> beans = bean.getBuildFeatureDescriptors();

    for (BuildFeatureBean b: beans) {
      SBuildFeatureDescriptor descriptor = b.getDescriptor();
      if (SharedResourcesPluginConstants.FEATURE_TYPE.equals(descriptor.getType())) {
        otherResourceNames.add(descriptor.getParameters().get(SharedResourcesPluginConstants.RESOURCE_PARAM_KEY));
      }
    }
    otherResourceNames.remove(myResourceName);
    myContext.putNamesInContext(otherResourceNames);
   *
   *
   *
   */

}
