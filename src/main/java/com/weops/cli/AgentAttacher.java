package com.weops.cli;

import com.weops.injector.GetAgentProperties;
import com.weops.injector.JavaAgentAttacher;
import com.weops.jvm.JvmDiscoverer;
import com.weops.jvm.JvmInfo;
import com.weops.utils.ProcessExecutionUtil;
import com.weops.user.UserRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

@Slf4j
public class AgentAttacher {

    private Arguments arguments;

    private Set<String> alreadySeenJvmPids = new HashSet<>();

    private UserRegistry userRegistry = UserRegistry.empty();

    public AgentAttacher(Arguments arguments) {
        this.arguments = arguments;
    }

    public void doAttach() {
        try {
            DiscoveryRules discoveryRules = arguments.getDiscoveryRules();
            if (!discoveryRules.isDiscoveryRequired() && arguments.isNoFork()) {
                attachToSpecificPidsAsCurrentUser();
            } else {
                discoverAndAttachLoop(discoveryRules);
            }
        } catch (Exception e) {
            log.error("Error during attachment", e);
        }
    }

    private void attachToSpecificPidsAsCurrentUser() throws Exception {
        // a shortcut for a simple usage of attaching to specific PIDs using the current user, which means we can avoid all
        // JVM discovery logic and user-switches
        Set<String> includePids = arguments.getIncludePids();
        for (String includePid : includePids) {
            Properties properties = GetAgentProperties.getAgentAndSystemProperties(includePid, userRegistry.getCurrentUser());
            attach(JvmInfo.withCurrentUser(includePid, properties));
        }
    }

    private void discoverAndAttachLoop(final DiscoveryRules discoveryRules) throws Exception {
        // fail fast if no attachment provider is working
        GetAgentProperties.getAgentAndSystemProperties(JvmInfo.CURRENT_PID, userRegistry.getCurrentUser());

        JvmDiscoverer jvmDiscoverer = new JvmDiscoverer.Compound(Arrays.asList(
                JvmDiscoverer.ForHotSpotVm.withDiscoveredTempDirs(userRegistry),
                new JvmDiscoverer.UsingPs(userRegistry))
        );

        while (true) {
            handleNewJvms(jvmDiscoverer.discoverJvms(), discoveryRules);
            if (!arguments.isContinuous()) {
                break;
            }
            Thread.sleep(1000);
        }
    }

    String toString(InputStream inputStream) throws IOException {
        try {
            Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            inputStream.close();
        }
    }

    private void handleNewJvms(Collection<JvmInfo> jvms, DiscoveryRules rules) {
        for (JvmInfo jvmInfo : jvms) {
            if (alreadySeenJvmPids.contains(jvmInfo.getPid())) {
                continue;
            }
            alreadySeenJvmPids.add(jvmInfo.getPid());
            if (!jvmInfo.isCurrentVM()) {
                try {
                    onJvmStart(jvmInfo, rules);
                } catch (Exception e) {
                    log.error("Unable to attach to JVM with PID = {}", jvmInfo.getPid(), e);
                }
            }
        }
    }

    private void onJvmStart(JvmInfo jvmInfo, DiscoveryRules discoveryRules) throws Exception {
        DiscoveryRules.DiscoveryRule firstMatch = discoveryRules.firstMatch(jvmInfo, userRegistry);
        if (firstMatch != null) {
            if (firstMatch.getMatchingType() == DiscoveryRules.MatcherType.INCLUDE) {
                log.info("Include rule {} matches for JVM {}", firstMatch, jvmInfo);
                onJvmMatch(jvmInfo);
            } else {
                log.info("Exclude rule {} matches for JVM {}", firstMatch, jvmInfo);
            }
        } else {
            log.info("No rule matches for JVM, thus excluding {}", jvmInfo);
        }
    }

    private void onJvmMatch(JvmInfo jvmInfo) throws Exception {
        if (arguments.isList()) {
            System.out.println(jvmInfo.toString(arguments.isListVmArgs()));
        } else {
            if (attach(jvmInfo)) {
                log.info("Done");
            } else {
                log.error("Unable to attach to JVM with PID = {}", jvmInfo.getPid());
            }
        }
    }

    private boolean attach(JvmInfo jvmInfo) throws Exception {
        final Map<String, String> agentArgs = getAgentArgs(jvmInfo);
        if (!agentArgs.containsKey("activation_method")) {
            agentArgs.put("activation_method", "APM_AGENT_ATTACH_CLI");
        }
        log.info("Attaching the Link agent to {} with arguments {}", jvmInfo, agentArgs);

        UserRegistry.User user = jvmInfo.getUser(userRegistry);
        if (user == null) {
            log.error("Could not load user {}", jvmInfo.getUserName());
            return false;
        }
        if (!jvmInfo.isVersionSupported()) {
            log.info("Cannot attach to JVM {} as the version {} is not supported.", jvmInfo, jvmInfo.getJavaVersion());
            return false;
        }
        if (jvmInfo.isAlreadyAttached()) {
            log.info("The agent is already attached to JVM {}", jvmInfo);
            return false;
        }
        if (user.isCurrentUser()) {
            JavaAgentAttacher.attach(jvmInfo.getPid(), agentArgs, arguments.getAgentJar());
            return true;
        } else if (user.canSwitchToUser()) {
            return attachAsUser(user, agentArgs, jvmInfo.getPid());
        } else {
            log.warn("Cannot attach to {} because the current user ({}) doesn't have the permissions to switch to user {}",
                    jvmInfo, UserRegistry.getCurrentUserName(), jvmInfo.getUserName());
            return false;
        }
    }

    private static boolean attachAsUser(UserRegistry.User user, Map<String, String> agentArgs, String pid) {

        List<String> args = new ArrayList<>();
        args.add("--include-pid");
        args.add(pid);
        for (Map.Entry<String, String> entry : agentArgs.entrySet()) {
            args.add("--config");
            args.add(entry.getKey() + "=" + entry.getValue());
        }
        ProcessExecutionUtil.CommandOutput output = user.executeAsUserWithCurrentClassPath(AgentAttacher.class, args);
        return output.exitedNormally();
    }

    private Map<String, String> getAgentArgs(JvmInfo jvmInfo) throws IOException, InterruptedException {
        if (arguments.getArgsProvider() != null) {
            LinkedHashMap<String, String> config = new LinkedHashMap<>();
            for (String conf : getArgsProviderOutput(jvmInfo).split(";")) {
                config.put(conf.substring(0, conf.indexOf('=')), conf.substring(conf.indexOf('=') + 1));
            }
            return config;
        } else {
            return arguments.getConfig();
        }
    }

    private String getArgsProviderOutput(JvmInfo jvmInfo) throws IOException, InterruptedException {
        final Process argsProvider = new ProcessBuilder(arguments.getArgsProvider(), jvmInfo.getPid()).start();
        if (argsProvider.waitFor() == 0) {
            return toString(argsProvider.getInputStream());
        } else {
            log.info("Not attaching the Link agent to {}, " +
                    "because the '--args-provider {}' script ended with a non-zero status code.", jvmInfo, arguments.getArgsProvider());
            throw new IllegalStateException(toString(argsProvider.getErrorStream()));
        }
    }

}