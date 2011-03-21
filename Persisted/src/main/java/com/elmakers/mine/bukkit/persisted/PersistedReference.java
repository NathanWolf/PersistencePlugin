package com.elmakers.mine.bukkit.persisted;

public interface PersistedReference
{
    public String getName();

    public PersistedClass getReferenceType();

    public Class<?> getType();

    public boolean isObject();
}
