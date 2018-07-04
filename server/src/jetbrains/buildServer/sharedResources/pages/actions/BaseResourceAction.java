package jetbrains.buildServer.sharedResources.pages.actions;

import com.intellij.openapi.diagnostic.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ConfigActionFactory;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.pages.Messages;
import jetbrains.buildServer.sharedResources.pages.ResourceHelper;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.web.openapi.ControllerAction;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class {@code BaseResourceAction}
 *
 * Defines base action for manipulating with resources as
 * well as contract for subclasses
 *
 * @see AddResourceAction
 * @see EditResourceAction
 * @see DeleteResourceAction
 * @see EnableDisableResourceAction
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class BaseResourceAction implements ControllerAction {

  @NotNull
  final Logger LOG = Logger.getInstance(BaseResourceAction.class.getName());

  @NotNull
  final ProjectManager myProjectManager;

  @NotNull
  final ResourceHelper myResourceHelper;

  @NotNull
  private final Messages myMessages;

  @NotNull
  final ConfigActionFactory myConfigActionFactory;

  @NotNull
  private final Resources myResources;

  @NotNull
  final ResourceProjectFeatures myProjectFeatures;


  protected BaseResourceAction(@NotNull final ProjectManager projectManager,
                               @NotNull final ResourceProjectFeatures projectFeatures,
                               @NotNull final ResourceHelper resourceHelper,
                               @NotNull final Messages messages,
                               @NotNull final ConfigActionFactory configActionFactory,
                               @NotNull final Resources resources) {
    myProjectManager = projectManager;
    myProjectFeatures = projectFeatures;
    myResourceHelper = resourceHelper;
    myMessages = messages;
    myConfigActionFactory = configActionFactory;
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

  void addMessage(@NotNull final HttpServletRequest request, @NotNull final String message) {
    myMessages.addMessage(request, message);
  }

  boolean containsDuplicateName(@NotNull final SProject project,
                                @NotNull final String resourceName) {
    return myResources.getAllOwnResources(project).stream()
                      .map(Resource::getName)
                      .anyMatch(resourceName::equals);
  }

  void createNameError(@NotNull final Element ajaxResponse, @NotNull final String name) {
    final ActionErrors errors = new ActionErrors();
    errors.addError("name", "Name " + name + " is already used by another resource");
    errors.serialize(ajaxResponse);
  }
}
