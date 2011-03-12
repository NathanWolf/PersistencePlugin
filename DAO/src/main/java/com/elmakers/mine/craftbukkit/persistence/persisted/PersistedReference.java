package com.elmakers.mine.craftbukkit.persistence.persisted;

public interface PersistedReference
{
	public PersistedClass getReferenceType();
	public boolean isObject();
	public String getName();
	public Class<?> getType();
}
