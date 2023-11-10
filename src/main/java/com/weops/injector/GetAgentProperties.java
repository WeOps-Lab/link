package com.weops.injector;


import com.weops.jvm.JvmAttachUtils;
import com.weops.jvm.JvmInfo;
import com.weops.utils.ProcessExecutionUtil;
import com.weops.user.UserRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

public class GetAgentProperties {

    /**
     * Prints the system and agent properties of the JVM with the provided pid to stdout,
     * it a way that can be consumed by {@link Properties#load(InputStream)}.
     * This works by attaching to the JVM with the provided pid and by calling
     * {@link com.sun.tools.attach.VirtualMachine#getSystemProperties()} and {@link com.sun.tools.attach.VirtualMachine#getAgentProperties()}.
     * <p>
     * In {@link #getAgentAndSystemPropertiesSwitchUser}, a new JVM is forked running this main method.
     * This JVM runs in the context of the same user that runs the JVM with the provided pid.
     * This indirection is needed as it's not possible to attach to a JVM that runs under a different user.
     * </p>
     *
     * @param args contains a single argument - the process id of the target VM
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        JvmAttachUtils.getAgentAndSystemProperties(args[0]).store(System.out, null);
    }

    /**
     * Attaches to the VM with the given pid and gets the agent and system properties.
     *
     * @param pid  The pid of the target VM, If it is current VM, this method will fork a VM for self-attachment.
     * @param user The user that runs the target VM. If this is not the current user, this method will fork a VM that runs under this user.
     * @return The agent and system properties of the target VM.
     * @throws Exception In case an error occurs while attaching to the target VM.
     */
    public static Properties getAgentAndSystemProperties(String pid, UserRegistry.User user) throws Exception {
        if (user.isCurrentUser() && !JvmInfo.CURRENT_PID.equals(pid)) {
            return JvmAttachUtils.getAgentAndSystemProperties(pid);
        } else {
            return getAgentAndSystemPropertiesSwitchUser(pid, user);
        }
    }

    static Properties getAgentAndSystemPropertiesSwitchUser(String pid, UserRegistry.User user) throws IOException {
        ProcessExecutionUtil.CommandOutput output = user.executeAsUserWithCurrentClassPath(GetAgentProperties.class, Arrays.asList(pid, user.getUsername()));
        if (output.getExitCode() == 0) {
            Properties properties = new Properties();
            properties.load(new StringReader(output.getOutput().toString()));
            return properties;
        } else {
            throw new RuntimeException(output.getOutput().toString(), output.getExceptionThrown());
        }
    }
}