package com.elmakers.mine.bukkit.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.elmakers.mine.bukkit.data.DataStore;

/**
 * Describes a schema.
 * 
 * This can be used to retrieve all persisted classes in a schema.
 * 
 * @author NathanWolf
 *
 */
public class Schema
{	
	public Schema(String name, DataStore defaultStore)
	{
		this.name = name;
		this.defaultStore = defaultStore;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void addPersistedClass(PersistentClass persistedClass)
	{
		persistedClasses.add(persistedClass);
		nameMap.put(persistedClass.getTableName(), persistedClass);
	}
	
	public List<PersistentClass> getPersistedClasses()
	{
		return persistedClasses;
	}
	
	public PersistentClass getPersistedClass(String className)
	{
		return nameMap.get(className);
	}
	
	public DataStore getStore()
	{
		return defaultStore;
	}
	
	public void disconnect()
	{
		if (defaultStore != null)
		{
			defaultStore.disconnect();
		}
	}

	private String									name;
	private DataStore								defaultStore;
	private final List<PersistentClass>				persistedClasses	= new ArrayList<PersistentClass>();
	private final HashMap<String, PersistentClass>	nameMap				= new HashMap<String, PersistentClass>();
}
