package dev.jco.upgrades;
public enum InteractionMode { LEFT, RIGHT, BOTH; public boolean accepts(InteractionMode actual){return this==BOTH||this==actual;} public static InteractionMode parse(String value){return valueOf(value.toUpperCase(java.util.Locale.ROOT));} }
