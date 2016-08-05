package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  private CustomResource(@NotNull final String id,
                         @NotNull final String projectId,
                         @NotNull final String name,
                         @NotNull final List<String> values,
                         boolean state) {
    super(id, projectId, name, ResourceType.CUSTOM, state);
    myValues = new ArrayList<>(values);
  }

  @NotNull
  static CustomResource newCustomResource(@NotNull final String id,
                                          @NotNull final String projectId,
                                          @NotNull final String name,
                                          @NotNull final List<String> values,
                                          boolean state) {
    return new CustomResource(id, projectId, name, values, state);
  }

  @NotNull
  public List<String> getValues() {
    return Collections.unmodifiableList(myValues);
  }

  @NotNull
  @Override
  public Map<String, String> getParameters() {
    final Map<String, String> result = super.getParameters();
    result.put("values", myValues.stream().collect(Collectors.joining("\n")));
    return result;
  }
}
