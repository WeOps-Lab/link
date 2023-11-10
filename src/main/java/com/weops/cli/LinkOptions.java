package com.weops.cli;

import org.apache.commons.cli.*;

import java.io.File;

public class LinkOptions {
    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();


    public Arguments parseOptions(String[] args) {
        Options options = buildOptions();

        Arguments arguments = new Arguments();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.getOptions().length == 0) {
                formatter.printHelp("AgentAttacher", options);
                System.exit(0);
            }

            if (cmd.hasOption("h")) {
                formatter.printHelp("AgentAttacher", options);
                System.exit(0);
            }

            if (cmd.hasOption("l")) {
                arguments.setList(true);
            }

            if (cmd.hasOption("v")) {
                arguments.setListVmArgs(true);
            }

            if (cmd.hasOption("c")) {
                arguments.setContinuous(true);
            }

            if (cmd.hasOption("no-fork")) {
                arguments.setNoFork(true);
            }

            if (cmd.hasOption("include-all")) {
                arguments.getDiscoveryRules().includeAll();
            }

            if (cmd.hasOption("include-pid")) {
                arguments.getDiscoveryRules().includePid(cmd.getOptionValue("include-pid"));
            }

            if (cmd.hasOption("include-main")) {
                arguments.getDiscoveryRules().includeMain(cmd.getOptionValue("include-main"));
            }

            if (cmd.hasOption("exclude-main")) {
                arguments.getDiscoveryRules().excludeMain(cmd.getOptionValue("exclude-main"));
            }

            if (cmd.hasOption("include-vmarg")) {
                arguments.getDiscoveryRules().includeVmArgs(cmd.getOptionValue("include-vmarg"));
            }

            if (cmd.hasOption("exclude-vmarg")) {
                arguments.getDiscoveryRules().excludeVmArgs(cmd.getOptionValue("exclude-vmarg"));
            }

            if (cmd.hasOption("include-user")) {
                arguments.getDiscoveryRules().includeUser(cmd.getOptionValue("include-user"));
            }

            if (cmd.hasOption("exclude-user")) {
                arguments.getDiscoveryRules().excludeUser(cmd.getOptionValue("exclude-user"));
            }

            if (cmd.hasOption("C")) {
                String[] configOptions = cmd.getOptionValues("C");
                for (String option : configOptions) {
                    String key = option.substring(0, option.indexOf('='));
                    String value = option.substring(option.indexOf('=') + 1);
                    arguments.getConfig().put(key, value);
                }
            }

            if (cmd.hasOption("A")) {
                arguments.setArgsProvider(cmd.getOptionValue("A"));
            }

            if (cmd.hasOption("agent-jar")) {
                arguments.setAgentJar(new File(cmd.getOptionValue("agent-jar")));
            }

        } catch (Exception e) {
            formatter.printHelp("AgentAttacher", options);
            System.exit(1);
        }
        return arguments;
    }

    private Options buildOptions() {
        Options options = new Options();

        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build();
        options.addOption(helpOption);

        Option listOption = Option.builder("l")
                .longOpt("list")
                .desc("List the JVMs that match the discovery rules")
                .build();
        options.addOption(listOption);

        Option listVmArgsOption = Option.builder("v")
                .longOpt("list-vmargs")
                .desc("Include JVM arguments when listing the JVMs")
                .build();
        options.addOption(listVmArgsOption);

        Option continuousOption = Option.builder("c")
                .longOpt("continuous")
                .desc("Continuously attach to matching JVMs")
                .build();
        options.addOption(continuousOption);

        Option noForkOption = Option.builder()
                .longOpt("no-fork")
                .desc("Do not fork a new process when attaching to JVMs")
                .build();
        options.addOption(noForkOption);

        Option includeAllOption = Option.builder()
                .longOpt("include-all")
                .desc("Include all JVMs for attachment")
                .build();
        options.addOption(includeAllOption);

        Option includePidOption = Option.builder()
                .longOpt("include-pid")
                .hasArgs()
                .desc("Include PIDs for attachment")
                .argName("pid")
                .build();
        options.addOption(includePidOption);

        Option includeMainOption = Option.builder()
                .longOpt("include-main")
                .hasArg()
                .desc("Include main class patterns for attachment")
                .argName("pattern")
                .build();
        options.addOption(includeMainOption);

        Option excludeMainOption = Option.builder()
                .longOpt("exclude-main")
                .hasArg()
                .desc("Exclude main class patterns for attachment")
                .argName("pattern")
                .build();
        options.addOption(excludeMainOption);

        Option includeVmargOption = Option.builder()
                .longOpt("include-vmarg")
                .hasArg()
                .desc("Include JVM argument patterns for attachment")
                .argName("pattern")
                .build();
        options.addOption(includeVmargOption);

        Option excludeVmargOption = Option.builder()
                .longOpt("exclude-vmarg")
                .hasArg()
                .desc("Exclude JVM argument patterns for attachment")
                .argName("pattern")
                .build();
        options.addOption(excludeVmargOption);

        Option includeUserOption = Option.builder()
                .longOpt("include-user")
                .hasArg()
                .desc("Include users for attachment")
                .argName("pattern")
                .build();
        options.addOption(includeUserOption);

        Option excludeUserOption = Option.builder()
                .longOpt("exclude-user")
                .hasArg()
                .desc("Exclude users for attachment")
                .argName("pattern")
                .build();
        options.addOption(excludeUserOption);

        Option configOption = Option.builder("C")
                .longOpt("config")
                .hasArg()
                .desc("Set agent configuration options")
                .argName("key=value")
                .valueSeparator('=')
                .build();
        options.addOption(configOption);

        Option argsProviderOption = Option.builder("A")
                .longOpt("args-provider")
                .hasArg()
                .desc("Set the program for providing agent args")
                .argName("args_provider_script")
                .build();
        options.addOption(argsProviderOption);

        Option agentJarOption = Option.builder()
                .longOpt("agent-jar")
                .hasArg()
                .desc("Set the agent JAR file path")
                .argName("file")
                .build();
        options.addOption(agentJarOption);

        return options;
    }
}
