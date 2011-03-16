package com.elmakers.mine.bukkit.plugins.mysql;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.elmakers.mine.bukkit.data.DataStore;
import com.elmakers.mine.bukkit.data.DataStoreProvider;
import com.elmakers.mine.bukkit.data.sql.MySQLStore;

/** 
 * A plugin to add a SQLite DataStore provider to the Persistence plugin
 * 
 * @author NathanWolf
 *
 */
public class MySQLPlugin extends JavaPlugin implements DataStoreProvider
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
		log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled");
	}

	private static final Logger	log	= Logger.getLogger("Minecraft");

	public String getType()
	{
		return "sqlite";
	}

	public DataStore createStore(String schema)
	{
		File dataFolder = getDataFolder();
		dataFolder.mkdirs();
		return new MySQLStore(schema, user, server, password);
	}
	
	public void intialize()
	{
		server = "localhost";
		user = "test";
		password = "test";
	}
	
	protected String	server		= null;
	protected String	user		= null;
	protected String	password	= null;
}
