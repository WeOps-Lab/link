package com.weops.cli;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Data
@RequiredArgsConstructor
public class Arguments {
    private DiscoveryRules discoveryRules = new DiscoveryRules();
    private Set includePids = new HashSet();
    private Map config = new HashMap();
    private String argsProvider;
    private boolean help;
    private boolean list;
    private boolean continuous;
    private boolean noFork;
    private boolean listVmArgs;
    private File agentJar;
}
