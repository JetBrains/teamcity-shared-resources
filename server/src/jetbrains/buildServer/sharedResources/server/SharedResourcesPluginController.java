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

  public SharedResourcesPluginController(
          @NotNull PluginDescriptor descriptor,
          @NotNull WebControllerManager web) {
    myDescriptor = descriptor;
    web.registerController(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_HTML), this);

  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request,
                                  @NotNull HttpServletResponse response) throws Exception {

    return new ModelAndView(myDescriptor.getPluginResourcesPath(EDIT_FEATURE_PATH_JSP));
  }


}
