package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.EditBuildTypeFormFactory;
import jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm;
import jetbrains.buildServer.controllers.buildType.ParameterInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.*;

/**
 * @author Oleg Rybak
 */
public class SharedResourcesPluginController extends BaseController {

  @NotNull
  private final PluginDescriptor myDescriptor;
  private EditBuildTypeFormFactory myFormFactory;

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
    final List<ParameterInfo> configParams = form.getBuildTypeParameters().getConfigurationParameters();
    final List<String> readLockNames = new ArrayList<String>();
    final List<String> writeLockNames = new ArrayList<String>();

    for (ParameterInfo param : configParams) {
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(param.getName());
      if (lock != null) {
        switch (lock.getType()) {
          case READ:
            readLockNames.add(lock.getName());
            break;
          case WRITE:
            writeLockNames.add(lock.getName());
            break;
        }
      }
    }
    final Map<String, Object> model = result.getModel();
    model.put(ATTR_READ_LOCKS, readLockNames);
    model.put(ATTR_WRITE_LOCKS, writeLockNames);
    return result;
  }


}
