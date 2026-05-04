package br.com.polarutilities.feature;

public interface PluginFeature {
    void enable();

    default void disable() {
    }
}
