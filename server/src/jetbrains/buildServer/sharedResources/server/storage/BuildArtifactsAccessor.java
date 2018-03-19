package jetbrains.buildServer.sharedResources.server.storage;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildArtifactsAccessor {

  @NotNull
  private static final Logger log = Logger.getInstance(BuildArtifactsAccessor.class.getName());

  @NotNull
  public Map<String, Lock> load(@NotNull final SBuild build) {
    final Map<String, Lock> result;
    final File artifact = new File(build.getArtifactsDirectory(), Storage.FILE_PATH);
    if (artifact.exists()) {
      result = new HashMap<>();
      try {
        final String content = FileUtil.readText(artifact, Storage.MY_ENCODING);
        final String[] lines = content.split("\\r?\\n");
        for (String line: lines) {
          final Lock lock = deserializeTakenLock(line);
          if (lock != null) {
            result.put(lock.getName(), lock);
          } else {
            if (log.isDebugEnabled()) {
              log.debug("Wrong locks storage format in file {" + artifact.getAbsolutePath() + "} line: {" + line + "}");
            }
          }
        }
      } catch(IOException e) {
        log.warn("Failed to load taken locks for build [" + build + "]; Message is: " + e.getMessage());
      }
    } else {
      result = Collections.emptyMap();
    }
    return result;
  }

  public void store(@NotNull final SBuild build, @NotNull final Map<Lock, String> takenLocks) throws IOException {
    final String content = takenLocks.entrySet().stream()
                                     .map(e -> serializeTakenLock(e.getKey(), e.getValue()))
                                     .collect(Collectors.joining("\n"));
    final File artifact = new File(build.getArtifactsDirectory(), Storage.FILE_PATH);
    if (FileUtil.createParentDirs(artifact)) {
      FileUtil.writeFile(artifact, content, Storage.MY_ENCODING);
    } else {
      log.warn("Failed to create parent dirs for file with taken locks for build {" + build + "}");
    }
  }

  @NotNull
  private String serializeTakenLock(@NotNull final Lock lock, @NotNull final String value) {
    return StringUtil.join("\t", lock.getName(), lock.getType(), value.equals("") ? " " : value);
  }

  @Nullable
  private Lock deserializeTakenLock(@NotNull final String line) {
    final List<String> strings = StringUtil.split(line, true, '\t'); // we need empty values for locks without values
    Lock result = null;
    if (strings.size() == 3) {
      String value =  StringUtil.trim(strings.get(2));
      if (value == null) {
        value = "";
      }
      result = new Lock(strings.get(0), LockType.byName(strings.get(1)), value);
    }
    return result;
  }
}
