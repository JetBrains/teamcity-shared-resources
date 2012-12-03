package jetbrains.buildServer.sharedResources.model.resources;

import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class Resource {

  @NotNull
  private final String myName;

  private final int myQuota;

  private Resource(@NotNull String name, int quota) {
    myName = name;
    myQuota = quota;
  }

  public boolean isInfinite() {
    return myQuota < 0;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  public int getQuota() {
    return myQuota;
  }

  public static Resource newResource(@NotNull String name, int quota) {
    return new Resource(name, quota);
  }

  public static Resource newInfiniteResource(@NotNull String name) {
    return new Resource(name, -1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Resource)) return false;
    Resource resource = (Resource) o;
    return myName.equals(resource.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
