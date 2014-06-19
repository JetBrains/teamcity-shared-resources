package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceFactory {

  @NotNull
  private final String myProjectId;

  /**
   * Returns {@code ResourceFactory} that constructs resources for priject with given {@code projectId}
   * @param projectId internal id of the project
   * @return {@code ResourceFactory} for project with given id
   */
  public static ResourceFactory getFactory(@NotNull final String projectId) {
    return new ResourceFactory(projectId);
  }

  private ResourceFactory(@NotNull final String projectId) {
    myProjectId = projectId;
  }

  /**
   * Creates new quoted resource with infinite quota
   *
   * @param name name of the resource
   * @param state state of the resource
   * @return new infinite quoted resource
   */
  @NotNull
  public Resource newInfiniteResource(@NotNull final String name, boolean state) {
    return QuotedResource.newInfiniteResource(myProjectId, name, state);
  }

  /**
   * Creates new quoted resource with limited quota
   *
   * @param name name of the resource
   * @param quota resource quota
   * @param state state of the resource
   * @return new quoted resource with limited quota
   */
  @NotNull
  public Resource newQuotedResource(@NotNull final String name, final int quota, boolean state) {
    return QuotedResource.newResource(myProjectId, name, quota, state);
  }

  /**
   * Creates new custom resource with specified value space
   *
   * @param name name of the resource
   * @param values values
   * @param state state of the resource
   * @return new custom resource with specified value space
   */
  @NotNull
  public Resource newCustomResource(@NotNull final String name, @NotNull final List<String> values, boolean state) {
    return CustomResource.newCustomResource(myProjectId, name, values, state);
  }

}
