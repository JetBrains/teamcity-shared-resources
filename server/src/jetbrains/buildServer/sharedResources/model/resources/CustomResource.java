package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code CustomResource}
 *
 * Represents resource with custom value space
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CustomResource extends AbstractResource {

  @NotNull
  private final List<String> myValues;

  private CustomResource(@NotNull final String name, @NotNull final List<String> values) {
    super(name, ResourceType.CUSTOM);
    myValues = new ArrayList<String>(values);
  }

  @NotNull
  static CustomResource newCustomResource(@NotNull final String name, @NotNull final List<String> values) {
    return new CustomResource(name, values);
  }

  @NotNull
  public List<String> getValues() {
    return Collections.unmodifiableList(myValues);
  }
}
