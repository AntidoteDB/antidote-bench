package adbm.docker.util;

import adbm.util.AdbmConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

import static adbm.util.helpers.FormatUtil.format;

public class DockerfileBuilder
{

    private static final Logger log = LogManager.getLogger(DockerfileBuilder.class);

    /**
     * Creates the Dockerfile that is used to build the Antidote Benchmark Image.
     * Overwrites the existing Dockerfile in case something was changed.
     *
     * @return true if the Dockerfile creation succeeds. Otherwise false.
     */
    public static boolean createDockerfile()
    {
        String dockerfile = getDockerfile();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(AdbmConstants.DOCKERFILE_PATH, false), "utf-8")))
        {
            writer.write(dockerfile);
            log.info("Dockerfile created!");
            return true;
        } catch (IOException e) {
            log.error("A error occurred while creating a Dockerfile!", e);
            return false;
        }
    }

    /**
     * Helper method to create the String of the Dockerfile.
     *
     * @return The String of the Dockerfile.
     */
    private static String getDockerfile()
    {
        return format("FROM {1}" +
                              "\n\n" +
                              "ENV HANDOFF_PORT \"8099\"" +
                              "\n" +
                              "ENV PB_PORT \"8087\"" +
                              "\n" +
                              "ENV PB_IP \"0.0.0.0\"" +
                              "\n" +
                              "ENV PBSUB_PORT \"8086\"" +
                              "\n" +
                              "ENV LOGREADER_PORT \"8085\"" +
                              "\n" +
                              "ENV RING_STATE_DIR \"data/ring\"" +
                              "\n" +
                              "ENV PLATFORM_DATA_DIR \"data\"" +
                              "\n" +
                              "ENV NODE_NAME \"antidote@127.0.0.1\"" +
                              "\n" +
                              "ENV SHORT_NAME \"false\"" +
                              "\n" +
                              "ENV ANTIDOTE_REPO \"{2}\"" +
                              "\n" +
                              "ENV ANTIDOTE_BRANCH \"master\"" +
                              "\n\n" +
                              "RUN set -xe" +
                              "{0}" +
                              "apt-get update" +
                              "{0}" +
                              "apt-get install -y --no-install-recommends git openssl ca-certificates" +
                              "{0}" +
                              "apt-get install -y dos2unix" +
                              "{0}" +
                              "cd /usr/src" +
                              "{0}" +
                              "git clone $ANTIDOTE_REPO" +
                              "{0}" +
                              "cd antidote" +
                              "{0}" +
                              "git checkout $ANTIDOTE_BRANCH" +
                              "{0}" +
                              "make rel" +
                              "{0}" +
                              "cp -R _build/default/rel/antidote /opt/" +
                              "{0}" +
                              "sed -e ''$i,'{'kernel, ['{'inet_dist_listen_min, 9100'}', '{'inet_dist_listen_max, 9100'}']'}''' /usr/src/antidote/_build/default/rel/antidote/releases/0.0.1/sys.config > /opt/antidote/releases/0.0.1/sys.config" +
                              "{0}" +
                              "rm -rf /var/lib/apt/lists/*" +
                              "\n\n" +
                              "COPY join_cluster_script.erl join_dcs_script.erl start_and_attach.sh /opt/antidote/" +
                              "\n" +
                              "COPY entrypoint.sh /" +
                              "\n\n" +
                              "RUN dos2unix /entrypoint.sh" +
                              "{0}" +
                              "find /opt/antidote -type f -print0 | xargs -0 dos2unix" +
                              "{0}" +
                              "apt-get --purge remove -y dos2unix" +
                              "{0}" +
                              "chmod a+x /opt/antidote/start_and_attach.sh" +
                              "{0}" +
                              "chmod a+x /entrypoint.sh" +
                              "\n\n" +
                              "# Distributed Erlang Port Mapper" +
                              "\n" +
                              "EXPOSE 4369" +
                              "\n" +
                              "# Ports for Antidote" +
                              "\n" +
                              "EXPOSE 8085 8086 8087 8099" +
                              "\n\n" +
                              "# Antidote RPC" +
                              "\n" +
                              "EXPOSE 9100" +
                              "\n\n" +
                              "VOLUME /opt/antidote/data" +
                              "\n\n" +
                              "ENTRYPOINT [\"/entrypoint.sh\"]" +
                              "\n\n" +
                              "CMD [\"/opt/antidote/start_and_attach.sh\"]",
                      " \\\n  && ", AdbmConstants.REQUIRED_IMAGE, AdbmConstants.AD_GIT_REPO_URL);
    }

}
