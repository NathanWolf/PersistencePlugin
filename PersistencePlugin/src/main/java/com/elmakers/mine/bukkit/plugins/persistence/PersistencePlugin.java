package com.elmakers.mine.bukkit.plugins.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockVector;

import com.elmakers.mine.bukkit.permission.GroupManager;
import com.elmakers.mine.bukkit.permission.PermissionManager;
import com.elmakers.mine.bukkit.persistence.CommandSenderData;
import com.elmakers.mine.bukkit.persistence.EntityInfo;
import com.elmakers.mine.bukkit.persistence.FieldInfo;
import com.elmakers.mine.bukkit.persistence.PersistedClass;
import com.elmakers.mine.bukkit.persistence.dao.PlayerData;
import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;
import com.elmakers.mine.bukkit.utilities.PluginUtilities;
import com.elmakers.mine.craftbukkit.persistence.Persistence;

/** 
 * The JavaPlugin interface for Persistence- binds Persistence to Bukkit.
 * 
 * @author NathanWolf
 *
 */
public class PersistencePlugin extends JavaPlugin
{
	/*
	 * Public API
	 */
	
	/**
	 * Default constructor! Hooray!
	 */
	public PersistencePlugin()
	{
	}
	
	/* Process player quit and join messages.
	 * 
	 * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] parameters)
	{
		if (listeners == null)
		{
			listeners = new ArrayList<Object>();
			listeners.add(handler);
			listeners.add(getPermissions());
		}
		return utilities.dispatch(listeners, sender, cmd.getName(), parameters);
	}

	/**
	 * Retrieve the singleton Persistence instance.
	 * 
	 * Use this function to get a reference to Persistence, which you can use to access the Persistence API.
	 * 
	 * @see com.elmakers.mine.craftbukkit.persistence.Persistence
	 * 
	 * @return The singleton instance of Persistence
	 */
	public Persistence getPersistence()
	{
		if (persistence == null)
		{
			persistence = new Persistence(getServer, getDataFolder());
			updateGlobalData();
		}
		return persistence;
	}
	

	protected void updateGlobalData()
	{
		// Update CommandSenders
		updateCommandSender("player" , Player.class);
		
		// Create BlockVector class
		EntityInfo vectorInfo = new EntityInfo("global", "vector");
		FieldInfo vectorId = new FieldInfo("id");
		FieldInfo fieldX = new FieldInfo("x");
		FieldInfo fieldY = new FieldInfo("y");
		FieldInfo fieldZ = new FieldInfo("z");
		
		// Make the hash code the id, make it readonly, and override its storage name
		vectorId.setIdField(true);
		vectorId.setReadOnly(true);
	
		// Bind each field- this is a little awkward right now, due to the
		// assymmetry (lack of setBlockX type setters).
		fieldX.setGetter("getBlockX");
		fieldY.setGetter("getBlockY");
		fieldZ.setGetter("getBlockZ");
		fieldX.setSetter("setX");
		fieldY.setSetter("setY");
		fieldZ.setSetter("setZ");
		
		// Create the class definition
		PersistedClass persistVector = getPersistedClass(BlockVector.class, vectorInfo);
		try
		{
			persistVector.persistField("hashCode", vectorId);
	
			persistVector.persistField("x", fieldX);
			persistVector.persistField("y", fieldY);
			persistVector.persistField("z", fieldZ);
			
			persistVector.validate();
		}
		catch (InvalidPersistedClassException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// TODO: Materials (? .. currently in Gameplay!)
	}
		
	protected CommandSenderData updateCommandSender(String senderId, Class<?> senderClass)
	{
		CommandSenderData sender = get(senderId, CommandSenderData.class);
		if (sender == null)
		{
			sender = new CommandSenderData(senderId, senderClass);
			put(sender);
		}		
		return sender;
	}

	/*
	 * Plugin interface
	 */

	/* Shut down Persistence, save data, clear cache
	 * 
	 * @see org.bukkit.plugin.Plugin#onDisable()
	 */
	public void onDisable()
	{
		if (persistence != null)
		{
			persistence.save();
			persistence.clear();
			persistence.disconnect();
		}
	}

	/* Start up Persistence, bind event handlers
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
	
	/*
	 * Helper functions
	 */
	
	/**
	 * Retrieve the Logger used by Persistence.
	 * 
	 * Currently, this is just the Minecraft server logger.
	 * 
	 * @return The Persistence logger
	 */
	public static Logger getLogger()
	{
		return log;
	}
	
	protected void initialize()
	{
		// Initialize permissions, if it hasn't been already
		getPermissions();
			
		handler.initialize(this, getPersistence(), getUtilities());
		listener.initialize(getPersistence(), handler);
		
		PluginManager pm = getServer().getPluginManager();
		
		pm.registerEvent(Type.PLAYER_QUIT, listener, Priority.Normal, this);
		pm.registerEvent(Type.PLAYER_JOIN, listener, Priority.Normal, this);
	}
	
	public PluginUtilities getUtilities()
	{
		if (utilities == null)
		{
			utilities = getPersistence().getUtilities(this);
		}
		
		return utilities;
	}
	
	public PermissionManager getPermissions()
	{
		if (permissions == null)
		{
			// TODO: This is messy, group manager relies on plugin utilities,
			// which needs a permission manager.
			// Hopefully all temporary!
			permissions = new GroupManager(getServer(), getPersistence(), getDataFolder());
			permissions.initialize();
			PlayerData.setPermissionHandler(permissions);
		}
		return permissions;
	}
	
	/**
	 * Retrieves a PluginUtilities interface for the specified plugin. 
	 * 
	 * Pass in your own plugin instance for access to data-driven in-game message strings and commands,
	 * and other useful utilities.
	 * 
	 * @param plugin The plugin for which to retrieve messages and commands
	 * @return A PluginUtilities instance for sending messages and processing commands
	 */
	public PluginUtilities getUtilities(Plugin plugin)
	{
		PluginUtilities utilities = new PluginUtilities(plugin, this);
		// TODO: This should be temporary...
		utilities.loadPermissions(PersistencePlugin.getInstance().getPermissions());
		return utilities;
	}
	
	/*
	 * Private data
	 */

	private static PersistencePlugin	pluginInstance	= null;
	private final PersistenceListener	listener		= new PersistenceListener();
	private final PersistenceCommands	handler			= new PersistenceCommands();
	private Persistence					persistence		= null;
	private GroupManager				permissions		= null;
	private PluginUtilities				utilities		= null;
	private List<Object>				listeners 		= null;
	private static final Logger			log				= Logger.getLogger("Minecraft");
	
}
