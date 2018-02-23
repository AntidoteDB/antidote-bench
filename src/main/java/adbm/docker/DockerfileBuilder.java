package adbm.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class DockerfileBuilder
{

    private static final Logger log = LogManager.getLogger(DockerfileBuilder.class);
    
    private static String getLocalDockerfile(String repoDirName)
    {
        if (repoDirName == null) repoDirName = "antidote";
        return "FROM erlang:19 \n" +
                "\n" +
                "ENV HANDOFF_PORT \"8099\" \n" +
                "ENV PB_PORT \"8087\" \n" +
                "ENV PB_IP \"0.0.0.0\" \n" +
                "ENV PBSUB_PORT \"8086\" \n" +
                "ENV LOGREADER_PORT \"8085\" \n" +
                "ENV RING_STATE_DIR \"data/ring\" \n" +
                "ENV PLATFORM_DATA_DIR \"data\" \n" +
                "ENV NODE_NAME \"antidote@127.0.0.1\" \n" +
                "ENV SHORT_NAME \"false\" \n" +
                "\n" +
                "COPY " + repoDirName + "/* /usr/src/" + repoDirName + "/\n" +
                "  \n" +
                "RUN set -xe \\\n" +
                "  && apt-get update \\\n" +
                "  && apt-get install -y --no-install-recommends git openssl ca-certificates \\\n" +
                "  && apt-get install -y dos2unix \\\n" +
                "  && cd /usr/src/" + repoDirName + " \\\n" +
                "  && find . -type f -print0 | xargs -0 dos2unix \\\n" +
                "  && make rel \\\n" +
                "  && cp -R _build/default/rel/antidote /opt/ \\\n" +
                "  && sed -e '$i,{kernel, [{inet_dist_listen_min, 9100}, {inet_dist_listen_max, 9100}]}' /usr/src/" + repoDirName + "/_build/default/rel/antidote/releases/0.0.1/sys.config > /opt/antidote/releases/0.0.1/sys.config \\\n" +
                "  && rm -rf /var/lib/apt/lists/*\n" +
                "\n" +
                "COPY join_cluster_script.erl join_dcs_script.erl start_and_attach.sh /opt/antidote/\n" +
                "COPY entrypoint.sh /\n" +
                "\n" +
                "RUN dos2unix /entrypoint.sh \\\n" +
                "  && find /opt/antidote -type f -print0 | xargs -0 dos2unix \\\n" +
                "  && apt-get --purge remove -y dos2unix \\\n" +
                "  && chmod a+x /opt/antidote/start_and_attach.sh \\\n" +
                "  && chmod a+x /entrypoint.sh\n" +
                "\n" +
                "# Distributed Erlang Port Mapper \n" +
                "\n" +
                "EXPOSE 4368 \n" +
                "\n" +
                "# Ports for Antidote \n" +
                "\n" +
                "EXPOSE 8085 8086 8087 8099 \n" +
                "\n" +
                "# Antidote RPC \n" +
                "\n" +
                "EXPOSE 9100 VOLUME /opt/antidote/data\n" +
                "ENTRYPOINT [\"/entrypoint.sh\"]\n" +
                "CMD [\"/opt/antidote/start_and_attach.sh\"]";
    }

    private static String getRemoteDockerfile()
    {
        return "FROM erlang:19\n" +
                "\n" +
                "ENV HANDOFF_PORT \"8099\"\n" +
                "ENV PB_PORT \"8087\"\n" +
                "ENV PB_IP \"0.0.0.0\"\n" +
                "ENV PBSUB_PORT \"8086\"\n" +
                "ENV LOGREADER_PORT \"8085\"\n" +
                "ENV RING_STATE_DIR \"data/ring\"\n" +
                "ENV PLATFORM_DATA_DIR \"data\"\n" +
                "ENV NODE_NAME \"antidote@127.0.0.1\"\n" +
                "ENV SHORT_NAME \"false\"\n" +
                "ENV ANTIDOTE_REPO \"https://github.com/SyncFree/antidote.git\"\n" +
                "ENV ANTIDOTE_BRANCH \"master\"\n" +
                "\n" +
                "RUN set -xe \\\n" +
                "  && apt-get update \\\n" +
                "  && apt-get install -y --no-install-recommends git openssl ca-certificates \\\n" +
                "  && apt-get install -y dos2unix \\\n" +
                "  && cd /usr/src \\\n" +
                "  && git clone $ANTIDOTE_REPO \\\n" +
                "  && cd antidote \\\n" +
                "  && git checkout $ANTIDOTE_BRANCH \\\n" +
                "  && make rel \\\n" +
                "  && cp -R _build/default/rel/antidote /opt/ \\\n" +
                "  && sed -e '$i,{kernel, [{inet_dist_listen_min, 9100}, {inet_dist_listen_max, 9100}]}' /usr/src/antidote/_build/default/rel/antidote/releases/0.0.1/sys.config > /opt/antidote/releases/0.0.1/sys.config \\\n" +
                "  && rm -rf /var/lib/apt/lists/*\n" +
                "\n" +
                "COPY join_cluster_script.erl join_dcs_script.erl start_and_attach.sh /opt/antidote/\n" +
                "COPY entrypoint.sh /\n" +
                "\n" +
                "RUN dos2unix /entrypoint.sh \\\n" +
                "  && find /opt/antidote -type f -print0 | xargs -0 dos2unix \\\n" +
                "  && apt-get --purge remove -y dos2unix \\\n" +
                "  && chmod a+x /opt/antidote/start_and_attach.sh \\\n" +
                "  && chmod a+x /entrypoint.sh\n" +
                "\n" +
                "# Distributed Erlang Port Mapper\n" +
                "EXPOSE 4368\n" +
                "# Ports for Antidote\n" +
                "EXPOSE 8085 8086 8087 8099\n" +
                "\n" +
                "# Antidote RPC\n" +
                "EXPOSE 9100\n" +
                "\n" +
                "VOLUME /opt/antidote/data\n" +
                "\n" +
                "ENTRYPOINT [\"/entrypoint.sh\"]\n" +
                "\n" +
                "CMD [\"/opt/antidote/start_and_attach.sh\"]";
    }

    public static void createDockerfile(boolean local)
    {
        //if (!GitManager.isReady()) return;
        //String gitRepoLocation = MapDBManager.getAppSetting(MapDBManager.GitRepoLocationSetting);
        //File gitRepoFolder = new File(gitRepoLocation);
        //File gitRepoParentFolder = gitRepoFolder.getParentFile();
        //if (gitRepoParentFolder == null) return;
        String dockerfile;
        //if (local) dockerfile = getLocalDockerfile(gitRepoFolder.getName());
        //else
            dockerfile = getRemoteDockerfile();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("Dockerfile/Dockerfile", false), "utf-8")))
        {
            writer.write(dockerfile);
            writer.close();
            log.info("Dockerfile created!");
            /*if (!local) return;
            File[] files = new File("Dockerfile").listFiles();
            if (files == null) return;
            for (File file : files) {
                //File dest = new File(gitRepoParentFolder, file.getName());
                File dest = new File("/", file.getName());
                Files.copy(
                        file.toPath(),
                        dest.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES,
                        LinkOption.NOFOLLOW_LINKS);
            }
            log.info("Dockerfile deployed!");*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
