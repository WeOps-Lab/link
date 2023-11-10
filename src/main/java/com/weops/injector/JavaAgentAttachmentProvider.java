package com.weops.injector;

import com.weops.injector.CachedAttachmentProvider;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.util.Arrays;
import java.util.List;

public class JavaAgentAttachmentProvider {
    private static ByteBuddyAgent.AttachmentProvider provider;

    private static ByteBuddyAgent.AttachmentProvider fallback;

    private synchronized static void init() {
        if (provider != null) {
            throw new IllegalStateException("CoreAttachmentProvider.init() should only be called once");
        }

        List<ByteBuddyAgent.AttachmentProvider> providers = Arrays.asList(
                ByteBuddyAgent.AttachmentProvider.ForModularizedVm.INSTANCE,
                ByteBuddyAgent.AttachmentProvider.ForJ9Vm.INSTANCE,
                new CachedAttachmentProvider(ByteBuddyAgent.AttachmentProvider.ForStandardToolsJarVm.JVM_ROOT),
                new CachedAttachmentProvider(ByteBuddyAgent.AttachmentProvider.ForStandardToolsJarVm.JDK_ROOT),
                new CachedAttachmentProvider(ByteBuddyAgent.AttachmentProvider.ForStandardToolsJarVm.MACINTOSH),
                new CachedAttachmentProvider(ByteBuddyAgent.AttachmentProvider.ForUserDefinedToolsJar.INSTANCE),
                // only use emulated attach last, as native attachment providers should be preferred
                getFallback());


        provider = new ByteBuddyAgent.AttachmentProvider.Compound(providers);
    }

    private synchronized static void initFallback() {
        if (fallback != null) {
            throw new IllegalStateException("CoreAttachmentProvider.initFallback() should only be called once");
        }
        fallback = ByteBuddyAgent.AttachmentProvider.ForEmulatedAttachment.INSTANCE;
    }

    public synchronized static ByteBuddyAgent.AttachmentProvider get() {
        if (provider == null) {
            init();
        }
        return provider;
    }

    public synchronized static ByteBuddyAgent.AttachmentProvider getFallback() {
        if (fallback == null) {
            initFallback();
        }
        return fallback;
    }
}
