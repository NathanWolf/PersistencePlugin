package com.elmakers.mine.bukkit.plugins.sqlite;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.data.DataStore;
import com.elmakers.mine.bukkit.data.DataStoreProvider;
import com.elmakers.mine.bukkit.data.sql.SQLiteStore;

/** 
 * A plugin to add a SQLite DataStore provider to the Persistence plugin
 * 
 * @author NathanWolf
 *
 */
public class SQLitePlugin extends JavaPlugin implements DataStoreProvider
{
	/*
	 * Public API
	 */

	/*
	 * Plugin interface
	 */

	/* TODO
	 * 
	 * @see org.bukkit.plugin.Plugin#onDisable()
	 */
	public void onDisable()
	{
		
	}

	/* Initialize this plugin
	 * 
	 * @see org.bukkit.plugin.Plugin#onEnable()
	 */
	public void onEnable()
	{
		PluginDescriptionFile pdfFile = this.getDescription();
		try
		{
			Plugin persistencePlugin = getServer().getPluginManager().getPlugin("Persistence");
			if (persistencePlugin == null)
			{
				log.info(pdfFile.getName() + " Requires the Persistence plugin");
				getServer().getPluginManager().disablePlugin(this);
				return;
			}
			dataFolder = persistencePlugin.getDataFolder();
	        log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled");
		}
		catch(Throwable e)
		{
	        log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " failed to initialize");
	        e.printStackTrace();
		}
	}

	private static final Logger	log	= Logger.getLogger("Minecraft");

	public String getType()
	{
		return "sqlite";
	}

	public DataStore createStore(String schema)
	{
		return new SQLiteStore(schema, dataFolder);
	}
	
	protected File dataFolder = null;
}
