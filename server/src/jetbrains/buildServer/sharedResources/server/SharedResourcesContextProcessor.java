package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesContextProcessor implements BuildStartContextProcessor {

  // where is it called?


  @Override
  public void updateParameters(@NotNull final BuildStartContext context) {
    // collect locks that are taken
    // determine value of lock to pass


  }

}
