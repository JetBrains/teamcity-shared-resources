

package jetbrains.buildServer.sharedResources.model.resources;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class QuotedResource extends AbstractResource {

  private static final int QUOTA_INFINITE = -1;

  private final int myQuota;

  private QuotedResource(@NotNull final String id,
                         @NotNull final String projectId,
                         @NotNull String name,
                         int quota,
                         boolean state) {
    super(id, projectId, name, ResourceType.QUOTED, state);
    myQuota = quota;
  }

  @NotNull
  static QuotedResource newResource(@NotNull final String id, @NotNull final String projectId, @NotNull String name, int quota, boolean state) {
    return new QuotedResource(id, projectId, name, quota, state);
  }

  @NotNull
  static QuotedResource newInfiniteResource(@NotNull final String id, @NotNull final String projectId, @NotNull String name, boolean state) {
    return new QuotedResource(id, projectId, name, QUOTA_INFINITE, state);
  }

  public boolean isInfinite() {
    return myQuota < 0;
  }

  public int getQuota() {
    return myQuota;
  }

  @NotNull
  @Override
  public Map<String, String> getParameters() {
    final Map<String, String> result =  super.getParameters();
    result.put("quota", Integer.toString(myQuota));
    return result;
  }
}