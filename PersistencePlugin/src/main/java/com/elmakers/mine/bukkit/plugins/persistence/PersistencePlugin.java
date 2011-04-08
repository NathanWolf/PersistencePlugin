package com.elmakers.mine.bukkit.plugins.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

import com.elmakers.mine.bukkit.data.DataStoreProvider;
import com.elmakers.mine.bukkit.groups.GroupManager;
import com.elmakers.mine.bukkit.permission.PermissionManager;
import com.elmakers.mine.bukkit.persisted.EntityInfo;
import com.elmakers.mine.bukkit.persisted.FieldInfo;
import com.elmakers.mine.bukkit.persisted.Persisted;
import com.elmakers.mine.bukkit.persistence.Persistence;
import com.elmakers.mine.bukkit.persistence.PersistentClass;
import com.elmakers.mine.bukkit.persistence.dao.CommandSenderData;
import com.elmakers.mine.bukkit.persistence.dao.PlayerData;
import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;
import com.elmakers.mine.bukkit.utilities.PluginUtilities;

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

    private static final Logger log             = Logger.getLogger("Minecraft");

    // TODO : support multiple perm files
    private static final String permissionsFile = "permissions.yml";


    // TODO : Use Persistence.persistenceMap to track one persistence instance
    // per server
    private Persistence                          persistence     = null;

    private DataStoreProvider                    defaultProvider = null;
    private GroupManager                         groups          = null;
    private final PersistenceCommands            handler         = new PersistenceCommands();
    private final PersistenceListener            listener        = new PersistenceListener();
    private List<Object>                         listeners       = null;
    private PermissionManager                    permissions     = null;
    private final Map<String, DataStoreProvider> providers       = new ConcurrentHashMap<String, DataStoreProvider>();
    private PluginUtilities                      utilities       = null;

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

    /**
     * Default constructor! Hooray!
     */
    public PersistencePlugin()
    {
    }

    /*
     * Private data
     */

    /**
     * Retrieves a PluginUtilities interface for the specified plugin.
     * 
     * Pass in your own plugin instance for access to data-driven in-game
     * message strings and commands, and other useful utilities.
     * 
     * @param plugin
     *            The plugin for which to retrieve messages and commands
     * @return A PluginUtilities instance for sending messages and processing
     *         commands
     */
    public PluginUtilities createUtilities(Plugin plugin)
    {
        return new PluginUtilities(plugin, persistence);
    }

    public GroupManager getGroups()
    {
        if (groups == null)
        {
            groups = new GroupManager(getServer(), getPersistence(), getUtilities(), getDataFolder());
        }
        return groups;
    }

    public PermissionManager getPermissions()
    {
        if (permissions == null)
        {
            permissions = new PermissionManager(getServer(), getPersistence());

            // TODO: This should be temporary...
            // This emulates some missing PluginLoader functionality from the
            // bukkit permissions branch
            // It scans config.yml for permissions
            PluginManager pm = getServer().getPluginManager();
            Plugin[] plugins = pm.getPlugins();
            for (Plugin plugin : plugins)
            {
                permissions.loadPluginPermissions(plugin);
            }

            permissions.loadPermissions(new File(getDataFolder(), permissionsFile));
            PlayerData.setPermissionHandler(permissions);
        }
        return permissions;
    }

    /**
     * Retrieve the singleton Persistence instance.
     * 
     * Use this function to get a reference to Persistence, which you can use to
     * access the Persistence API.
     * 
     * @see com.elmakers.mine.craftbukkit.persistence.Persistence
     * 
     * @return The singleton instance of Persistence
     */
    public Persistence getPersistence()
    {
        if (persistence == null)
        {
            // Search for DataStore providers
            PluginManager pm = this.getServer().getPluginManager();
            Plugin[] plugins = pm.getPlugins();
            for (Plugin plugin : plugins)
            {
                if (plugin instanceof DataStoreProvider)
                {
                    DataStoreProvider provider = (DataStoreProvider) plugin;
                    providers.put(provider.getType(), provider);
                    if (defaultProvider == null)
                    {
                        defaultProvider = provider;
                    }
                }
            }

            if (defaultProvider == null)
            {
                log.severe("Persistence: No data store providers found!");
                log.severe("Persistence: Please install a data store provider, such as SQLitePlugin.jar");
            }
            else
            {
                persistence = new Persistence(getServer(), defaultProvider);
                Persisted.setPersistence(getServer(), persistence);
                updateGlobalData();
            }
        }
        return persistence;
    }

    public PluginUtilities getUtilities()
    {
        if (utilities == null)
        {
            utilities = createUtilities(this);
        }

        return utilities;
    }

    protected void initialize()
    {
        // Initialize the groups and permission managers, if they aren't alrady
        getGroups();
        getPermissions();

        handler.initialize(this, getPersistence(), getUtilities());
        listener.initialize(getPersistence(), handler);

        PluginManager pm = getServer().getPluginManager();

        pm.registerEvent(Type.PLAYER_QUIT, listener, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_JOIN, listener, Priority.Normal, this);
    }

    /*
     * Process player quit and join messages.
     * 
     * @see
     * org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] parameters)
    {
        if (listeners == null)
        {
            listeners = new ArrayList<Object>();
            listeners.add(handler);
            listeners.add(getGroups());
        }
        return utilities.dispatch(listeners, sender, cmd.getName(), parameters);
    }

    /*
     * Shut down Persistence, save data, clear cache
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

    /*
     * Start up Persistence, bind event handlers
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
        catch (Throwable e)
        {
            PluginDescriptionFile pdfFile = this.getDescription();
            log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + " failed to initialize");
            e.printStackTrace();
        }
    }

    protected CommandSenderData updateCommandSender(String senderId, Class<?> senderClass)
    {
        CommandSenderData sender = persistence.get(senderId, CommandSenderData.class);
        if (sender == null)
        {
            sender = new CommandSenderData(senderId, senderClass);
            persistence.put(sender);
        }
        return sender;
    }

    protected void updateGlobalData()
    {
        // Update CommandSenders
        updateCommandSender("player", Player.class);

        // Create BlockVector class
        EntityInfo vectorInfo = new EntityInfo("global", "vector");
        FieldInfo vectorId = new FieldInfo("id");
        FieldInfo fieldX = new FieldInfo("x");
        FieldInfo fieldY = new FieldInfo("y");
        FieldInfo fieldZ = new FieldInfo("z");

        // Make the hash code the id, make it readonly, and override its storage
        // name
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
        PersistentClass persistVector = persistence.getPersistedClass(BlockVector.class, vectorInfo);
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
}
