package com.elmakers.mine.bukkit.persistence;

public interface PersistedReference
{
	public PersistedClass getReferenceType();
	public boolean isObject();
	public String getName();
	public Class<?> getType();
}
