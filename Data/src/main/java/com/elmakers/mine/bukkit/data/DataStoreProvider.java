package com.elmakers.mine.bukkit.data;

public interface DataStoreProvider
{
    public DataStore createStore(String schema);

    public String getType();
}
