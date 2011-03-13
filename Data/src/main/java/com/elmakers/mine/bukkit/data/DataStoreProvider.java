package com.elmakers.mine.bukkit.data;

public interface DataStoreProvider
{
	public String getType();
	public DataStore createStore(String schema);
}
