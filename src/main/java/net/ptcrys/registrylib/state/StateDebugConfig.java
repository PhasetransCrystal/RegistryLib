package net.ptcrys.registrylib.state;

import java.util.function.UnaryOperator;

public class StateDebugConfig {

    private boolean enabled;
    private boolean writable;
    private int readPermission = 2;
    private int writePermission = 4;

    public static StateDebugConfig disabled() {
        return new StateDebugConfig();
    }

    public static StateDebugConfig createEnabled() {
        return new StateDebugConfig().enabled(true);
    }

    public StateDebugConfig enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public StateDebugConfig writable(boolean writable) {
        this.writable = writable;
        return this;
    }

    public StateDebugConfig readPermission(int readPermission) {
        this.readPermission = checkPermission(readPermission);
        return this;
    }

    public StateDebugConfig writePermission(int writePermission) {
        this.writePermission = checkPermission(writePermission);
        return this;
    }

    private static int checkPermission(int permission) {
        if (permission < 0 || permission > 4) {
            throw new IllegalArgumentException("Permission must be between 0 and 4: " + permission);
        }
        return permission;
    }

    public StateDebugConfig configure(UnaryOperator<StateDebugConfig> config) {
        return config.apply(this);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean writable() {
        return writable;
    }

    public int readPermission() {
        return readPermission;
    }

    public int writePermission() {
        return writePermission;
    }
}
