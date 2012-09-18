package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * Date: 18.09.12
 * Time: 15:57
 *
 * @author Oleg Rybak
 */
public abstract class AbstractLock implements Lock {

  private final String name;

  protected AbstractLock(String name) {
    this.name = name;
  }

  @NotNull
  public final String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractLock)) return false;
    AbstractLock that = (AbstractLock) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
