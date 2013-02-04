package jetbrains.buildServer.sharedResources.model.resources;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public enum ResourceType {

  /**
   * Resource with quota without custom values.
   * Quota can be infinite
   */
  QUOTED,

  /**
   * Resource that has custom value space
   */
  CUSTOM

}
