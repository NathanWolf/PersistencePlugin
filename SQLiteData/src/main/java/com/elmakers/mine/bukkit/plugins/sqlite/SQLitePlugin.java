package com.elmakers.mine.bukkit.plugins.sqlite;

import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

/** 
 * A plugin to add a SQLite DataStore provider to the Persistence plugin
 * 
 * @author NathanWolf
 *
 */
public class SQLitePlugin extends JavaPlugin
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
		try
		{
			initialize();
			PluginDescriptionFile pdfFile = this.getDescription();
	        log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled");
		}
		catch(Throwable e)
		{
			PluginDescriptionFile pdfFile = this.getDescription();
	        log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " failed to initialize");
	        e.printStackTrace();
		}
	}
	
	protected void initialize()
	{
		// TODO
	}

	private static final Logger	log	= Logger.getLogger("Minecraft");
	
}
