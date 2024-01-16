

package jetbrains.buildServer.sharedResources.server.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public final class DuplicateResourceException extends Exception {
  public DuplicateResourceException(@NotNull final String name) {
    super("Resource with name " + name + " already exists");
  }
}