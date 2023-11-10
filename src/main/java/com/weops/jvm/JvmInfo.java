package com.weops.jvm;

import com.weops.user.UserRegistry;

import java.util.Locale;
import java.util.Properties;

public class JvmInfo {
    public static final String CURRENT_PID = JvmAttachUtils.CURRENT_PID;

    private final String pid;
    private final String userName;
    private final String mainClass;
    private final String vmArgs;
    private final String javaVersion;
    private final String mainArgs;
    private boolean alreadyAttached;

    JvmInfo(String pid, String userName, Properties properties) {
        this.pid = pid;
        this.userName = userName;
        // intentionally not storing all properties to reduce the risk of exposing secrets in heap dumps
        String sunJavaCommand = properties.getProperty("sun.java.command");
        if (sunJavaCommand != null && !sunJavaCommand.isEmpty()) {
            // mirrors the behavior of jps -l
            // spaces in directory names are problematic just as they are with jps
            int firstSpace = sunJavaCommand.indexOf(' ');
            if (firstSpace > 0) {
                this.mainClass = sunJavaCommand.substring(0, firstSpace);
                this.mainArgs = sunJavaCommand.substring(firstSpace + 1);
            } else {
                this.mainClass = sunJavaCommand;
                this.mainArgs = null;
            }
        } else {
            mainClass = null;
            mainArgs = null;
        }
        this.vmArgs = properties.getProperty("sun.jvm.args");
        this.javaVersion = properties.getProperty("java.version");
        this.alreadyAttached = properties.containsKey("Link.attached");
    }

    public static JvmInfo withCurrentUser(String pid, Properties properties) {
        return of(pid, UserRegistry.getCurrentUserName(), properties);
    }

    public static JvmInfo of(String pid, String userName, Properties properties) {
        return new JvmInfo(pid, userName, properties);
    }

    public static boolean isJ9() {
        return System.getProperty("java.vm.name", "").toUpperCase(Locale.US).contains("J9");
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean listVmArgs) {
        return getPid() + ' ' + mainClass + (listVmArgs ? ' ' + vmArgs : "");
    }

    public String getPid() {
        return pid;
    }

    public boolean isCurrentVM() {
        return getPid().equals(CURRENT_PID);
    }

    public String getUserName() {
        return userName;
    }

    public UserRegistry.User getUser(UserRegistry registry) {
        return registry.get(userName);
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getVmArgs() {
        return vmArgs;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public boolean isVersionSupported() {
        // new scheme introduced in java 9, thus we can use it as a shortcut
        if (javaVersion.startsWith("1.")) {
            return Character.digit(javaVersion.charAt(2), 10) >= 7;
        } else {
            return true;
        }
    }

    public boolean isAlreadyAttached() {
        return alreadyAttached;
    }

    public boolean isCurrentUser() {
        return userName.equals(UserRegistry.getCurrentUserName());
    }
}
