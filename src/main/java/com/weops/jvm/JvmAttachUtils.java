package com.weops.jvm;

import com.weops.injector.JavaAgentAttachmentProvider;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.util.Properties;

public class JvmAttachUtils {
    public static final String CURRENT_PID = ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve();

    public static Properties getAgentAndSystemProperties(String pid) {
        ByteBuddyAgent.AttachmentProvider.Accessor accessor = JavaAgentAttachmentProvider.get().attempt();
        if (!accessor.isAvailable()) {
            throw new IllegalStateException("No compatible attachment provider is available");
        }

        try {
            Class<?> vm = accessor.getVirtualMachineType();
            Object virtualMachineInstance = vm
                    .getMethod("attach", String.class)
                    .invoke(null, pid);
            try {
                Properties agentProperties = (Properties) vm.getMethod("getAgentProperties").invoke(virtualMachineInstance);
                Properties systemProperties = (Properties) vm.getMethod("getSystemProperties").invoke(virtualMachineInstance);
                systemProperties.putAll(agentProperties);
                return systemProperties;
            } finally {
                vm.getMethod("detach").invoke(virtualMachineInstance);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error during attachment using: " + accessor, e);
        }
    }
}
