package com.elmakers.mine.bukkit.persisted;

import org.bukkit.Server;

public interface PersistedClass
{
    public EntityInfo getEntityInfo();

    public Object getIdData(Object o);

    public Persistence getPersistence();

    public Server getServer();

    public Class<? extends Object> getType();
}
