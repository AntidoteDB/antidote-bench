package adbm.docker.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;

public class DockerUtil {

    private static final Logger log = LogManager.getLogger(DockerUtil.class);

    /**
     * If the first character of the name is '/' it is removed and then the name is returned.
     * This character is sometimes added by Docker for unknown reasons.
     *
     * @param containerNameFromDocker The name of the container.
     * @return The name where the first character is removed if it was a '/'.
     */
    public static String normalizeName(String containerNameFromDocker) {
        if (containerNameFromDocker.startsWith("/")) return containerNameFromDocker.substring(1);
        else return containerNameFromDocker;
    }

    /**
     * @param containerNames
     * @param containerId
     * @return
     */
    @Nonnull
    public static String getFirstNameOfContainer(List<String> containerNames, String containerId) {
        if (containerNames != null && containerNames.size() > 0) {
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
