package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

/**
 * A class to encapsulate data for a plugin.
 * 
 * Each plugin can register any number of messages and commands.
 * 
 * @author NathanWolf
 * 
 */
@PersistClass(schema = "global", name = "plugin")
public class PluginData extends Persisted
{

    protected List<String>                   authors;
    // Command / message cache- transient
    protected HashMap<String, PluginCommand> commandMap = new HashMap<String, PluginCommand>();
    protected List<PluginCommand>            commands   = new ArrayList<PluginCommand>();
    protected String                         description;
    protected String                         id;
    protected List<MaterialList>             materials;

    protected HashMap<String, Message>       messageMap = new HashMap<String, Message>();
    protected List<Message>                  messages   = new ArrayList<Message>();
    protected String                         version;
    protected String                         website;

    public PluginData()
    {
    }

    public PluginData(Plugin plugin)
    {
        update(plugin);
    }

    @PersistField
    public List<String> getAuthors()
    {
        return authors;
    }

    public PluginCommand getCommand(String commandName, String defaultTooltip, String defaultUsage, CommandSenderData sender, PermissionType pType)
    {
        // First, look for a root command by this name-
        // command map only holds root commands!
        PluginCommand command = commandMap.get(commandName);
        if (command != null)
        {
            command.setPermissionType(pType);
            List<CommandSenderData> senders = command.getSenders();
            if (sender != null)
            {
                if (senders == null)
                {
                    senders = new ArrayList<CommandSenderData>();
                }
                if (!senders.contains(sender))
                {
                    senders.add(sender);
                    command.setSenders(senders);
                }
            }
            return command;
        }

        // Create a new un-parented command
        command = new PluginCommand(this, commandName, defaultTooltip, pType);
        command.addUsage(defaultUsage);

        if (sender != null)
        {
            command.addSender(sender);
        }

        getPersistence().put(command);
        commandMap.put(commandName, command);
        commands.add(command);

        return command;
    }

    public List<PluginCommand> getCommands()
    {
        return commands;
    }

    @PersistField
    public String getDescription()
    {
        return description;
    }

    @PersistField(id = true)
    public String getId()
    {
        return id;
    }

    public MaterialList getMaterialList(String listId)
    {
        MaterialList list = null;
        for (MaterialList checkList : materials)
        {
            if (checkList.getId().equalsIgnoreCase(listId))
            {
                list = checkList;
                break;
            }
        }

        if (list == null)
        {
            list = new MaterialList(listId);
        }

        return list;
    }

    @PersistField
    public List<MaterialList> getMaterials()
    {
        return materials;
    }

    public Message getMessage(String messageId, String defaultValue)
    {
        // First, look up existing message
        Message message = messageMap.get(messageId);
        if (message != null)
        {
            return message;
        }

        // Create a new message
        message = new Message(this, messageId, defaultValue);
        messageMap.put(messageId, message);
        messages.add(message);

        getPersistence().put(message);
        return message;
    }

    public List<Message> getMessages()
    {
        return messages;
    }

    @PersistField
    public String getVersion()
    {
        return version;
    }

    @PersistField
    public String getWebsite()
    {
        return website;
    }

    public void initializeCache(List<Message> allMessages, List<PluginCommand> allCommands)
    {
        for (Message message : allMessages)
        {
            if (message.getPlugin().getId().equalsIgnoreCase(id))
            {
                messageMap.put(message.getMessageId(), message);
                messages.add(message);
            }
        }

        // Only cache root commands- the rest are in tree form
        for (PluginCommand command : allCommands)
        {
            if (command.getParent() == null && command.getPlugin().getId().equalsIgnoreCase(id))
            {
                commandMap.put(command.getCommand(), command);
                commands.add(command);
            }
        }
    }

    public void setAuthors(List<String> authors)
    {
        this.authors = authors;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setMaterials(List<MaterialList> materials)
    {
        this.materials = materials;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public void setWebsite(String website)
    {
        this.website = website;
    }

    public void update(Plugin plugin)
    {
        PluginDescriptionFile pdfFile = plugin.getDescription();
        id = pdfFile.getName();
        version = pdfFile.getVersion();
        description = pdfFile.getDescription();
        authors = new ArrayList<String>();
        if (authors == null)
        {
            authors = new ArrayList<String>();
        }
        authors.addAll(pdfFile.getAuthors());
        website = pdfFile.getWebsite();
    }
}
