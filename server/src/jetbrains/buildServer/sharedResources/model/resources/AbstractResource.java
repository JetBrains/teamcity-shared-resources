

package jetbrains.buildServer.sharedResources.model.resources;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class AbstractResource implements Resource {

  @NotNull
  private final String myName;

  @NotNull
  private final String myProjectId;

  @NotNull
  private final ResourceType myType;

  @NotNull
  private final String myId;

  private final boolean myState;

  AbstractResource(@NotNull final String id,
                   @NotNull final String projectId,
                   @NotNull final String name,
                   @NotNull final ResourceType type,
                   boolean state) {
    myId = id;
    myName = name;
    myProjectId = projectId;
    myType = type;
    myState = state;
  }

  @NotNull
  @Override
  public final String getName() {
    return myName;
  }

  @NotNull
  @Override
  public final ResourceType getType() {
    return myType;
  }

  @Override
  public final boolean isEnabled() {
    return myState;
  }

  @Override
  @NotNull
  public String getProjectId() {
    return myProjectId;
  }

  @Override
  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  @Override
  public Map<String, String> getParameters() {
    return CollectionsUtil.asMap(
            "type", myType.name().toLowerCase(),
            "name", myName,
            "enabled", Boolean.toString(myState)
    );
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final AbstractResource that = (AbstractResource)o;
    return myProjectId.equals(that.myProjectId) &&
           myId.equals(that.myId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myProjectId, myId);
  }

  @Override
  public String toString() {
    return "Resource{" +
           "name='" + myName + '\'' +
           ", projectId='" + myProjectId + '\'' +
           ", type=" + myType +
           ", id='" + myId + '\'' +
           ", state=" + myState +
           ", parameters=[" + getParameters().entrySet().stream().map(p -> p.getKey() + "=" + p.getValue()).collect(Collectors.joining(", ")) +
           "]}";
  }
}