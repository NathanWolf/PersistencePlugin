package com.elmakers.mine.bukkit.persisted;

public interface PersistedReference
{
	public PersistedClass getReferenceType();
	public boolean isObject();
	public String getName();
	public Class<?> getType();
}
