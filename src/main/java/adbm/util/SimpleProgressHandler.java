package adbm.util;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;

public class SimpleProgressHandler implements ProgressHandler
{
    private static final Logger log = LogManager.getLogger(SimpleProgressHandler.class);

    private String type;

    public SimpleProgressHandler(String type) {
        if (type != null) this.type = type;
        else this.type = "ID";
    }

    @Override
    public void progress(ProgressMessage message) throws DockerException
    {
        if (message.error() != null) {
            handleError(message.error());
        }
        else if (message.progressDetail() != null) {
            handleProgress(message.id(), message.status(), message.progress());
        }
    }

    void handleProgress(String id, String status, String progress)
    {
        if (progress == null) {
            log.info(MessageFormat.format(type + " {0}: {1}", id, status));
        }
        else {
            log.info(MessageFormat.format(type + " {0}: {1} {2}", id, status, progress));
        }
    }

    void handleError(String error) throws DockerException
    {
        log.error(error);
        throw new DockerException(error);
    }
}
