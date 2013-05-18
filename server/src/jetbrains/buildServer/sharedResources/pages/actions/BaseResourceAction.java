package jetbrains.buildServer.sharedResources.pages.actions;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * Class {@code BaseResourceAction}
 *
 * Defines base action for manipulating with resources as
 * well as contract for subclasses
 *
 * @see AddResourceAction
 * @see EditResourceAction
 * @see DeleteResourceAction
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class BaseResourceAction implements ControllerAction {

  protected final Logger LOG = Logger.getInstance(BaseResourceAction.class.getName());

  @NotNull
  protected final ProjectManager myProjectManager;

  @NotNull
  protected final Resources myResources;

  protected BaseResourceAction(@NotNull final ProjectManager projectManager,
                               @NotNull final Resources resources) {
    myProjectManager = projectManager;
    myResources = resources;
  }

  @NotNull
  public abstract String getActionName();

  @Override
  public final boolean canProcess(@NotNull final HttpServletRequest request) {
    return getActionName().equals(request.getParameter("action"));
  }

  @Override
  public void process(@NotNull final HttpServletRequest request,
                      @NotNull final HttpServletResponse response,
                      @Nullable final Element ajaxResponse) {
    if (ajaxResponse == null) {
      Loggers.SERVER.debug("Error: ajaxResponse is null");
    } else {
      doProcess(request, response, ajaxResponse);
    }
  }

  protected abstract void doProcess(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response,
                                    @NotNull final Element ajaxResponse);

  @Nullable
  protected Resource getResourceFromRequest(@NotNull final HttpServletRequest request) {
    final String resourceName = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
    final ResourceType resourceType = ResourceType.fromString(request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE));
    Resource resource = null;
    if (ResourceType.QUOTED.equals(resourceType)) {
      final String resourceQuota = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA);
      if (resourceQuota != null && !"".equals(resourceQuota)) { // we have quoted resource
        try {
          int quota = Integer.parseInt(resourceQuota);
          resource = ResourceFactory.newQuotedResource(resourceName, quota);
        } catch (IllegalArgumentException e) {
          LOG.warn("Illegal argument supplied in quota for resource [" + resourceName + "]");
        }
      } else { // we have infinite resource
        resource = ResourceFactory.newInfiniteResource(resourceName);
      }
    } else if (ResourceType.CUSTOM.equals(resourceType)) {
      final String values = request.getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_VALUES);
      final Collection<String> strings = StringUtil.split(values, true, '\r', '\n');
      resource = ResourceFactory.newCustomResource(resourceName, strings);
    }
    return resource;
  }

  protected void createNameError(@NotNull final Element ajaxResponse, @NotNull final String name) {
    final ActionErrors errors = new ActionErrors();
    errors.addError("name", "Name " + name + " is already used");
    errors.serialize(ajaxResponse);
  }
}
