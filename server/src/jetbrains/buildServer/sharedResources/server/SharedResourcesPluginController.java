package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.BuildFeatureBean;
import jetbrains.buildServer.controllers.admin.projects.BuildFeaturesBean;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
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
import java.util.Set;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.EDIT_FEATURE_PATH_HTML;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.EDIT_FEATURE_PATH_JSP;
import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_PARAM_KEY;

/**
 *
 * @author Oleg Rybak
 */
public class SharedResourcesPluginController extends BaseController {

  private static final Logger LOG = Logger.getInstance(SharedResourcesPluginController.class.getName());

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
    final String myResourceName = bean.getPropertiesBean().getProperties().get(RESOURCE_PARAM_KEY);
    final Collection<BuildFeatureBean> beans = bean.getBuildFeatureDescriptors();

    for (BuildFeatureBean b: beans) {
      FeatureUtil.extractResource(otherResourceNames, b.getDescriptor());
    }

    otherResourceNames.remove(myResourceName);
    myContext.putNamesInContext(otherResourceNames);
    return result;
  }
}
