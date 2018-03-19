package jetbrains.buildServer.sharedResources.server.storage;

import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public interface Storage {

  @NotNull
  String FILE_PARENT = ".teamcity/" + SharedResourcesPluginConstants.PLUGIN_NAME;

  @NotNull
  String FILE_NAME = "taken_locks.txt";

  @NotNull
  String FILE_PATH = FILE_PARENT + "/" + FILE_NAME;

  @NotNull
  String MY_ENCODING = "UTF-8";
}
