package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

/**
 * Created with IntelliJ IDEA.
 * Date: 17.09.12
 * Time: 14:55
 *
 * @author Oleg Rybak
 */
public final class NamedLock extends AbstractLock implements Lock {

  public static NamedLock createNamedLock(@NotNull String name) {
    return new NamedLock(name);
  }

  private NamedLock(@NotNull String name) {
    super(name);
  }

}
