package com.elmakers.mine.bukkit.persisted;

import org.bukkit.Server;

public interface PersistedClass
{
	public Object getIdData(Object o);
	public Server getServer();
	public Persistence getPersistence();
	public Class<? extends Object> getType();
	public EntityInfo getEntityInfo();
}
