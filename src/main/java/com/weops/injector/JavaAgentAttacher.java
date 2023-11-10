package com.weops.injector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.ByteBuddyAgent;

@Slf4j
public class JavaAgentAttacher {

    /**
     * This key is very short on purpose.
     * The longer the agent argument ({@code -javaagent:<path>=<args>}), the greater the chance that the max length of the agent argument is reached.
     * Because of a bug in the {@linkplain ByteBuddyAgent.AttachmentProvider.ForEmulatedAttachment emulated attachment},
     * this can even lead to segfaults.
     */
    private static final String TEMP_PROPERTIES_FILE_KEY = "c";


    /**
     * Store configuration to a temporary file
     *
     * @param configuration agent configuration
     * @param folder        temporary folder, use {@literal null} to use default
     * @return created file if any, {@literal null} if none was created
     */
    static File createTempProperties(Map<String, String> configuration, File folder) {
        File tempFile = null;
        if (!configuration.isEmpty()) {
            Properties properties = new Properties();
            properties.putAll(configuration);

            try {
                tempFile = File.createTempFile("link", ".tmp", folder);
                try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    properties.store(outputStream, null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tempFile;
    }

    /**
     * Attaches the agent to a remote JVM
     *
     * @param pid           the PID of the JVM the agent should be attached on
     * @param configuration the agent configuration
     * @param agentJarFile  the agent jar file
     */
    public static void attach(String pid, Map<String, String> configuration, File agentJarFile) {
        // making a copy of provided configuration as user might have used an immutable map impl.
        Map<String, String> config = new HashMap<>(configuration);
        if (!config.containsKey("activation_method")) {
            config.put("activation_method", "PROGRAMMATIC_SELF_ATTACH");
        }
        File tempFile = createTempProperties(config, null);
        String agentArgs = tempFile == null ? null : TEMP_PROPERTIES_FILE_KEY + "=" + tempFile.getAbsolutePath();

        attachWithFallback(agentJarFile, pid, agentArgs);
        if (tempFile != null) {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private static void attachWithFallback(File agentJarFile, String pid, String agentArgs) {
        try {
            // while the native providers may report to be supported and appear to work properly, in practice there are
            // cases (Docker without '--init' option on some JDK images like 'openjdk:8-jdk-alpine') where the accessor
            // returned by the provider will not work as expected at attachment time.
            ByteBuddyAgent.attach(agentJarFile, pid, agentArgs, JavaAgentAttachmentProvider.get());
        } catch (RuntimeException e1) {
            try {
                ByteBuddyAgent.attach(agentJarFile, pid, agentArgs, JavaAgentAttachmentProvider.getFallback());
            } catch (RuntimeException e2) {
                // output the two exceptions for debugging
                log.error("Unable to attach with fallback provider:", e2);

                log.error("Unable to attach with regular provider:", e1);
            }
        }
    }

}
