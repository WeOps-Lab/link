package com.weops;

import com.weops.cli.AgentAttacher;
import com.weops.cli.Arguments;
import com.weops.cli.LinkOptions;

public class Main {

    public static void main(String[] args) {
        LinkOptions linkOptions = new LinkOptions();
        Arguments arguments = linkOptions.parseOptions(args);
        AgentAttacher agentAttacher = new AgentAttacher(arguments);
        agentAttacher.doAttach();
    }
}
