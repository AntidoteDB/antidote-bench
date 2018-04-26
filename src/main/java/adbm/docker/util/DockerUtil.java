package adbm.docker.util;

import adbm.util.EverythingIsNonnullByDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@EverythingIsNonnullByDefault
public class DockerUtil {

    private static final Logger log = LogManager.getLogger(DockerUtil.class);

    /**
     * If the first character of the name is '/' it is removed and then the name is returned.
     * This character is sometimes added by Docker for unknown reasons.
     *
     * @param containerNameFromDocker The name of the container.
     * @return The name where the first character is removed if it was a '/'.
     */
    private static String normalizeName(String containerNameFromDocker) {
        if (containerNameFromDocker.startsWith("/")) return containerNameFromDocker.substring(1);
        else return containerNameFromDocker;
    }

    /**
     * Gets the first name of a container.
     * Logs if the container has multiple names or if the container does not have a name (although this should not be possible).
     * Also logs the name of the container before and after the normalization.
     *
     * @param containerNames The list of names of the container (should normally contain one name)
     * @param containerId The id of the container (for logging)
     * @return The first name of the container.
     */
    public static String getFirstNameOfContainer(List<String> containerNames, String containerId) {
        if (containerNames.size() > 0) {
            log.trace("Container (id: {}) first name before normalization: {}", containerId, containerNames.get(0));
            String firstName = normalizeName(containerNames.get(0));
            if (containerNames.size() > 1) {
                StringBuilder nameString = new StringBuilder();
                for (String name : containerNames) {
                    nameString.append(normalizeName(name));
                    nameString.append(", ");
                }
                String names = nameString.toString();
                if (names.length() > 2) {
                    log.debug(
                            "The container (id: {}) has multiple names: {}", containerId, names
                                    .substring(0, names.length() - 3));
                }
            }
            log.trace("Container (id: {}) first name after normalization: {}", containerId, firstName);
            return firstName;
        } else {
            log.error("The container (id: {}) does not have a name! This should not be possible!", containerId);
            return "";
        }
    }

}
