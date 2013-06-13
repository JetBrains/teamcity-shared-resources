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
   * @param name name of the resource
   * @return new infinite quoted resource
   */
  @NotNull
  public static Resource newInfiniteResource(@NotNull final String name) {
    return QuotedResource.newInfiniteResource(name);
  }

  /**
   * Creates new quoted resource with limited quota
   * @param name name of the resource
   * @param quota resource quota
   * @return new quoted resource with limited quota
   */
  @NotNull
  public static Resource newQuotedResource(@NotNull final String name, final int quota) {
    return QuotedResource.newResource(name, quota);
  }

  /**
   * Creates new custom resource with specified value space
   * @param name name of the resource
   * @param values values
   * @return new custom resource with specified value space
   */
  @NotNull
  public static Resource newCustomResource(@NotNull final String name, @NotNull final List<String> values) {
    return CustomResource.newCustomResource(name, values);
  }

}
