package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceFactory {

  /**
   * Creates new quoted resource with infinite quota
   *
   * @param name name of the resource
   * @param state state of the resource
   * @return new infinite quoted resource
   */
  @NotNull
  public static Resource newInfiniteResource(@NotNull final String name, boolean state) {
    return QuotedResource.newInfiniteResource(name, state);
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
  public static Resource newQuotedResource(@NotNull final String name, final int quota, boolean state) {
    return QuotedResource.newResource(name, quota, state);
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
  public static Resource newCustomResource(@NotNull final String name, @NotNull final List<String> values, boolean state) {
    return CustomResource.newCustomResource(name, values, state);
  }

}
