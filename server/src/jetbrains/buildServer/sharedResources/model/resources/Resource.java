package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interface {@code Resource}
 *
 * Common methods for all resources
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Resource {

  /**
   * Returns unique name of the resource
   * @return name of the resource
   */
  @NotNull
  String getName();

  /**
   * Returns type of the resource
   * @return type of the resource
   */
  @NotNull
  ResourceType getType();

  /**
   * State of the resource {@code enabled} / {@code disabled}
   * @return state of the resource {@code enabled} / {@code disabled}
   */
  boolean isEnabled();

  /**
   * Converts resource to the set of project feature parameters
   *
   * @return resource as properties
   * @since 10.0
   */
  @NotNull
  Map<String, String> getParameters();
}
