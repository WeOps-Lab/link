package com.weops.injector;

import net.bytebuddy.agent.ByteBuddyAgent;

public class CachedAttachmentProvider implements ByteBuddyAgent.AttachmentProvider {
    private volatile Accessor accessor;
    private final ByteBuddyAgent.AttachmentProvider delegate;

    CachedAttachmentProvider(ByteBuddyAgent.AttachmentProvider delegate) {
        this.accessor = delegate.attempt();
        this.delegate = delegate;
    }

    public Accessor attempt() {
        if (accessor == null) {
            accessor = delegate.attempt();
        }
        return accessor;
    }
}
