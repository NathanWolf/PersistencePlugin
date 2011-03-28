package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

/**
 * A data class for encapsulating and storing a Command object.
 * 
 * @author NathanWolf
 * 
 */

@PersistClass(name = "command", schema = "global")
public class PluginCommand extends Persisted implements
        Comparable<PluginCommand>
{
    private static final String                  indent   = "  ";

    private String                               callbackMethod;

    // Transient data
    private final HashMap<String, PluginCommand> childMap = new HashMap<String, PluginCommand>();

    private List<PluginCommand>                  children;

    private String                               command;

    private boolean                              enabled  = true;

    private int                                  id;

    private PluginCommand                        parent;

    private String                               permissionNode;

    private PermissionType                       permissionType;

    private PluginData                           plugin;

    private List<CommandSenderData>              senders;

    private String                               tooltip;

    private List<String>                         usage;

    /**
     * The default constructor, used by Persistence to create instances.
     * 
     * Use PluginUtilities to create PluginCommands.
     * 
     * @see com.elmakers.mine.bukkit.utilities.PluginUtilities#getCommand(String,
     *      String, String, CommandSenderData, String, PermissionType)
     */
    public PluginCommand()
    {

    }

    protected PluginCommand(PluginData plugin, String commandName, String tooltip, PermissionType pType)
    {
        this.plugin = plugin;
        this.command = commandName;
        this.tooltip = tooltip;
        this.permissionType = pType;
    }

    /**
     * Use this to add an additional command sender that is able to receive this
     * type of message.
     * 
     * @param sender
     *            the command sender to add
     */
    public void addSender(CommandSenderData sender)
    {
        if (sender == null)
        {
            return;
        }

        if (senders == null)
        {
            senders = new ArrayList<CommandSenderData>();
        }
        if (!senders.contains(sender))
        {
            senders.add(sender);
        }
    }

    /**
     * Add a command to this command as a sub-command.
     * 
     * Sub-commands are activated using parameters. So:
     * 
     * /persist list global.player.NathanWolf
     * 
     * Consists of the main Command "persist", one sub-command "list", and one
     * parameter "global.player.NathanWolf".
     * 
     * @param command
     *            The command to add as a sub-command of this one
     */
    protected void addSubCommand(PluginCommand command)
    {
        if (children == null)
        {
            children = new ArrayList<PluginCommand>();
        }

        // Child will self-register!
        command.setParent(this);

        // Pass on any senders
        if (senders != null)
        {
            for (CommandSenderData sender : senders)
            {
                command.addSender(sender);
            }
        }
    }

    protected void addToParent()
    {
        if (parent != null)
        {
            if (parent.children == null)
            {
                parent.children = new ArrayList<PluginCommand>();
            }
            if (parent.childMap.get(command) == null)
            {
                parent.children.add(this);
                parent.childMap.put(command, this);
            }
        }
    }

    /**
     * Use this to add an additional usage (example) string to this command.
     * 
     * @param use
     *            The usage string
     */
    public void addUsage(String use)
    {
        if (use == null || use.length() <= 0)
        {
            return;
        }

        if (usage == null)
        {
            usage = new ArrayList<String>();
        }
        if (!usage.contains(use))
        {
            usage.add(use);
        }
    }

    /**
     * Set up automatic command binding for this command.
     * 
     * If you dispatch commands with messaging.dispatch, this command will
     * automatically call the given method on the listener class if executed.
     * 
     * For Player commands, the signature should be:
     * 
     * public boolean onMyCommand(Player player, String[] parameters) { }
     * 
     * For General commands, a CommandSender should be used in place of Player.
     * 
     * @param methodName
     * @see com.elmakers.mine.bukkit.utilities.PluginUtilities#dispatch(Object,
     *      CommandSender, String, String[])
     */
    public void bind(String methodName)
    {
        callbackMethod = methodName;
    }

    /**
     * Check to see if this command matches a given command string.
     * 
     * If the command sender is a player, a permissions check will be done.
     * 
     * @param sender
     *            the sender requesting access.
     * @param commandString
     *            The command string to check
     * @return Whether or not the command succeeded
     */
    public boolean checkCommand(CommandSender sender, String commandString)
    {
        return command.equals(commandString) || command.equals(commandString.toLowerCase());
    }

    public boolean checkPermission(CommandSender sender)
    {
        Player player = null;
        PlayerData playerData = null;
        if (sender instanceof Player)
        {
            player = (Player) sender;
            playerData = getPersistence().get(player.getName(), PlayerData.class);
        }

        if (player == null)
        {
            return true;
        }

        if (playerData == null)
        {
            // This should probably never happen...
            return false;
        }

        String pnode = getPermissionNode();
        if (pnode != null && pnode.length() > 0)
        {
            return playerData.isSet(pnode);
        }

        return true;
    }

    public int compareTo(PluginCommand compare)
    {
        return command.compareTo(compare.getCommand());
    }

    public String getCallbackMethod()
    {
        return callbackMethod;
    }

    public List<PluginCommand> getChildren()
    {
        return children;
    }

    @PersistField
    public String getCommand()
    {
        return command;
    }

    public String getDefaultPermissionNode()
    {
        String pNode = "";
        PluginCommand addParent = parent;
        while (addParent != null)
        {
            pNode = addParent.command + "." + pNode;
            addParent = addParent.parent;
        }
        pNode = plugin.getId() + ".commands." + pNode + command;
        return pNode;
    }

    @PersistField(id = true, auto = true)
    public int getId()
    {
        return id;
    }

    public PluginCommand getParent()
    {
        return parent;
    }

    protected String getPath()
    {
        String path = command;
        if (parent != null)
        {
            path = parent.getPath() + " " + path;
        }
        return path;
    }

    public String getPermissionNode()
    {
        if (permissionNode == null)
        {
            permissionNode = getDefaultPermissionNode();
        }
        return permissionNode;
    }

    public PermissionType getPermissionType()
    {
        return permissionType;
    }

    public PluginData getPlugin()
    {
        return plugin;
    }

    public List<CommandSenderData> getSenders()
    {
        return senders;
    }

    /**
     * Get or create a sub-command of this command.
     * 
     * @param subCommandName
     *            The sub-command name
     * @param defaultTooltip
     *            The default tooltip
     * @param defaultUsage
     *            The default usage string
     * @return A new command object
     */
    public PluginCommand getSubCommand(String subCommandName, String defaultTooltip, String defaultUsage)
    {
        return getSubCommand(subCommandName, defaultTooltip, defaultUsage, PermissionType.DEFAULT);
    }

    /**
     * Get or create a sub-command of this command.
     * 
     * @param subCommandName
     *            The sub-command name
     * @param defaultTooltip
     *            The default tooltip
     * @param defaultUsage
     *            The default usage string
     * @param pNode
     *            The permission node to use
     * @param pType
     *            The type of permissions to apply
     * @return A new command object
     */
    public PluginCommand getSubCommand(String subCommandName, String defaultTooltip, String defaultUsage, PermissionType pType)
    {
        PluginCommand child = childMap.get(subCommandName);
        if (child == null)
        {
            child = new PluginCommand(plugin, subCommandName, defaultTooltip, pType);
            child.addUsage(defaultUsage);

            // adds senders
            addSubCommand(child);
            getPersistence().put(child);
        }

        child.setPermissionType(pType);

        return child;
    }

    public String getTooltip()
    {
        return tooltip;
    }

    public List<String> getUsage()
    {
        return usage;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    protected void removeFromParent()
    {
        if (parent != null)
        {
            if (parent.childMap.get(command) != null)
            {
                parent.children.remove(this);
                parent.childMap.remove(command);
            }
        }
    }

    /**
     * Use this to display a help message for this command to the given sender.
     * 
     * CommandSender may be a player, server console, etc.
     * 
     * @param sender
     *            The CommandSender (e.g. Player) to display help to
     * @param prefix
     *            A prefix, such as "Use: " to put in front of the first line
     * @param showUsage
     *            Whether or not to show detailed usage information
     * @param showSubCommands
     *            Whether or not to also display a tree of sub-command usage
     */
    public void sendHelp(CommandSender sender, String prefix, boolean showUsage, boolean showSubCommands)
    {
        boolean useSlash = sender instanceof Player;
        String slash = useSlash ? "/" : "";
        String currentIndent = "";

        if (callbackMethod != null)
        {
            String message = currentIndent + slash + getPath() + " : " + tooltip;
            sender.sendMessage(prefix + message);
            currentIndent += indent;

            if (showUsage && usage != null)
            {
                for (String exampleUse : usage)
                {
                    sender.sendMessage(currentIndent + " ex: " + getPath() + " " + exampleUse);
                }
            }
        }

        if (showSubCommands && children != null)
        {
            for (PluginCommand child : children)
            {
                child.sendHelp(sender, "", showUsage, showSubCommands);
            }
        }
    }

    /**
     * Use to send a short informational help message
     * 
     * This can be used when the player has mis-entered parameters or some other
     * exceptional case.
     * 
     * @param sender
     *            The CommandSender to reply to
     */
    public void sendShortHelp(CommandSender sender)
    {
        sendHelp(sender, "Use: ", false, false);
    }

    public void sendUse(CommandSender sender)
    {
        sendHelp(sender, "Use: ", true, true);
    }

    public void setCommand(String command)
    {
        // Must do this here too, since we maintain a hash of sub-commands by
        // command name!
        removeFromParent();
        this.command = command;
        addToParent();
    }

    @PersistField
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    @PersistField
    public void setParent(PluginCommand parent)
    {
        removeFromParent();
        this.parent = parent;
        addToParent();
    }

    public void setPermissionType(PermissionType permissionType)
    {
        this.permissionType = permissionType;
    }

    @PersistField
    public void setPlugin(PluginData plugin)
    {
        this.plugin = plugin;
    }

    @PersistField
    public void setSenders(List<CommandSenderData> senders)
    {
        this.senders = senders;
    }

    @PersistField
    public void setTooltip(String tooltip)
    {
        this.tooltip = tooltip;
    }

    @PersistField
    public void setUsage(List<String> usage)
    {
        this.usage = usage;
    }
}
