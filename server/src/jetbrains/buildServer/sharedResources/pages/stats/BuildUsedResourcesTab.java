package jetbrains.buildServer.sharedResources.pages.stats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AccessDeniedException;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.storage.BuildArtifactsAccessor;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildUsedResourcesTab extends ViewLogTab {

  @NotNull
  private final BuildArtifactsAccessor myAccessor;

  @NotNull
  private final Resources myResources;

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final ProjectManager myProjectManager;

  public BuildUsedResourcesTab(@NotNull final PagePlaces pagePlaces,
                               @NotNull final SBuildServer server,
                               @NotNull final PluginDescriptor descriptor,
                               @NotNull final BuildArtifactsAccessor accessor,
                               @NotNull final Resources resources,
                               @NotNull final SharedResourcesFeatures features) {
    super("Shared Resources", "buildUsedResources", pagePlaces, server);
    myAccessor = accessor;
    myResources = resources;
    myFeatures = features;
    myProjectManager = myServer.getProjectManager();

    setIncludeUrl(descriptor.getPluginResourcesPath("usedResources.jsp"));
  }

  @Override
  protected void fillModel(@NotNull final Map<String, Object> model,
                           @NotNull final HttpServletRequest request,
                           @NotNull final SBuild build) {

    final Map<String, Lock> locks = myAccessor.load(build);      // <- todo: only values?

    final String projectId = build.getProjectId();
    Map<String, Resource> resources;
    if (projectId != null) {
      resources = myResources.getResourcesMap(projectId);
    } else {
      resources = Collections.emptyMap();
    }

    Map<String, SProject> resourceOrigins = new HashMap<>();

    resources.values().forEach(resource -> {
      try {
        SProject project = myProjectManager.findProjectById(resource.getProjectId());
        if (project != null) {
          resourceOrigins.put(resource.getName(), project);
        }
      } catch (AccessDeniedException ignored) {}
    });

    model.put("locks", locks);
    model.put("resources", resources);
    model.put("resourceOrigins", resourceOrigins);
  }


  @Override
  protected boolean isAvailable(@NotNull final HttpServletRequest request, @NotNull final SBuild build) {
    boolean result = super.isAvailable(request, build);
    if (result) {
      SBuildType buildType = build.getBuildType();
      if (buildType != null) {
        return myFeatures.searchForFeatures(buildType).stream()
                         .map(SharedResourcesFeature::getLockedResources)
                         .anyMatch(m -> !m.isEmpty());
      }
    }
    return result;
  }
}
