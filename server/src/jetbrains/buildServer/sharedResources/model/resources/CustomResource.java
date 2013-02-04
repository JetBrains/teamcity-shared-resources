package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code CustomResource}
 *
 * Represents resource with custom value space
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CustomResource extends AbstractResource {

  @NotNull
  private final Set<String> myValues;

  private CustomResource(@NotNull final String name, @NotNull final Collection<String> values) {
    super(name, ResourceType.CUSTOM);
    myValues = new HashSet<String>(values);
  }

  static CustomResource newCustomResource(@NotNull final String name, @NotNull final Collection<String> values) {
    return new CustomResource(name, values);
  }

  @NotNull
  public Set<String> getValues() {
    return Collections.unmodifiableSet(myValues);
  }
}
