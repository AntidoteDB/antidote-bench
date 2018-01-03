package adbm.util;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;

import java.text.MessageFormat;

public class SimpleProgressHandler implements ProgressHandler
{
    private String _type;

    public SimpleProgressHandler(String type) {
        if (type != null) _type = type;
        else _type = "ID";
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
            System.out.println(MessageFormat.format(_type + " {0}: {1}", id, status));
        }
        else {
            System.out.println(MessageFormat.format(_type + " {0}: {1} {2}", id, status, progress));
        }
    }

    void handleError(String error) throws DockerException
    {
        System.out.println(error);
        throw new DockerException(error);
    }
}
