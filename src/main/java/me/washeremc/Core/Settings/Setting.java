package me.washeremc.Core.Settings;

import java.util.function.Function;

public class Setting<T> {
private final String key;
private final T defaultValue;
private final String displayName;
private final Function<T, T> toggleFunction;

public Setting(String key, T defaultValue, String displayName, Function<T, T> toggleFunction) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.displayName = displayName;
    this.toggleFunction = toggleFunction;
}

public String getKey() {
    return key;
}

public T getDefaultValue() {
    return defaultValue;
}

public String getDisplayName() {
    return displayName;
}

public T toggle(T currentValue) {
    return toggleFunction.apply(currentValue);
}
}