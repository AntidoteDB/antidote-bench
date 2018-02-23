package adbm.docker;

public class DockerUtil
{

    /**
     * If the first character of the name is '/' it is removed and then the name is returned.
     * This character is sometimes added by Docker for unknown reasons.
     *
     * @param containerNameFromDocker The name of the container.
     * @return The name where the first character is removed if it was a '/'.
     */
    public static String normalizeName(String containerNameFromDocker)
    {
        if (containerNameFromDocker.startsWith("/")) return containerNameFromDocker.substring(1);
        else return containerNameFromDocker;
    }

}
