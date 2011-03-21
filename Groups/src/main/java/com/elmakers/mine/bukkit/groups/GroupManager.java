package com.elmakers.mine.bukkit.groups;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import com.elmakers.mine.bukkit.permission.dao.ProfileData;
import com.elmakers.mine.bukkit.persisted.Persistence;
import com.elmakers.mine.bukkit.persistence.dao.Group;
import com.elmakers.mine.bukkit.persistence.dao.Message;
import com.elmakers.mine.bukkit.persistence.dao.PlayerData;
import com.elmakers.mine.bukkit.persistence.dao.PluginCommand;
import com.elmakers.mine.bukkit.utilities.PluginUtilities;

public class GroupManager
{
    protected static final Logger   log = Logger.getLogger("Minecraft");

    private Message                 addedPlayerToGroupMessage;

    private Message                 createdGroupMessage;

    private Message                 denyAccessMessage;

    private PluginCommand           denyCommand;

    private PluginCommand           denyGroupCommand;

    private PluginCommand           denyPlayerCommand;

    private Message                 grantAccessMessage;

    private PluginCommand           grantCommand;

    private PluginCommand           grantGroupCommand;
    private PluginCommand           grantPlayerCommand;
    private PluginCommand           groupAddCommand;

    private PluginCommand           groupCommand;
    private PluginCommand           groupCreateCommand;
    private Message                 groupExistsMessage;
    private Message                 groupNotFoundMessage;
    private PluginCommand           groupRemoveCommand;
    protected final Persistence     persistence;
    private Message                 playerNotFoundMessage;
    private Message                 removedPlayerFromGroupMessage;
    protected final Server          server;
    private Message                 unknownProfileMessage;

    protected final PluginUtilities utilities;

    public GroupManager(Server server, Persistence persistence,
            PluginUtilities utilities, File dataFolder)
    {
        this.persistence = persistence;
        this.server = server;
        this.utilities = utilities;
        initialize();
    }

    public void initialize()
    {
        GroupManagerDefaults d = new GroupManagerDefaults();

        // Messages
        addedPlayerToGroupMessage = utilities.getMessage("addedPlayerToGroup", d.addedPlayerToGroupMessage);
        removedPlayerFromGroupMessage = utilities.getMessage("removedPlayerFromGroup", d.removedPlayerFromGroupMessage);
        createdGroupMessage = utilities.getMessage("createdGroup", d.createdGroupMessage);
        denyAccessMessage = utilities.getMessage("denyAccess", d.denyAccessMessage);
        grantAccessMessage = utilities.getMessage("grantAccess", d.grantAccessMessage);
        groupExistsMessage = utilities.getMessage("groupExistss", d.groupExistsMessage);
        playerNotFoundMessage = utilities.getMessage("playerNotFound", d.playerNotFoundMessage);
        groupNotFoundMessage = utilities.getMessage("groupNotFound", d.groupNotFoundMessage);
        unknownProfileMessage = utilities.getMessage("unknownProfile", d.unknownProfileMessage);

        // Commands
        groupCommand = utilities.getGeneralCommand(d.groupCommand[0], d.groupCommand[1], d.groupCommand[2]);
        groupCreateCommand = groupCommand.getSubCommand(d.groupCreateCommand[0], d.groupCreateCommand[1], d.groupCreateCommand[2]);
        groupAddCommand = groupCommand.getSubCommand(d.groupAddCommand[0], d.groupAddCommand[1], d.groupAddCommand[2]);
        groupRemoveCommand = groupCommand.getSubCommand(d.groupRemoveCommand[0], d.groupRemoveCommand[1], d.groupRemoveCommand[2]);

        denyCommand = utilities.getGeneralCommand(d.denyCommand[0], d.denyCommand[1], d.denyCommand[2]);
        denyPlayerCommand = denyCommand.getSubCommand(d.denyPlayerCommand[0], d.denyPlayerCommand[1], d.denyPlayerCommand[2]);
        denyGroupCommand = denyCommand.getSubCommand(d.denyGroupCommand[0], d.denyGroupCommand[1], d.denyGroupCommand[2]);

        grantCommand = utilities.getGeneralCommand(d.grantCommand[0], d.grantCommand[1], d.grantCommand[2]);
        grantPlayerCommand = grantCommand.getSubCommand(d.grantPlayerCommand[0], d.grantPlayerCommand[1], d.grantPlayerCommand[2]);
        grantGroupCommand = grantCommand.getSubCommand(d.grantGroupCommand[0], d.grantGroupCommand[1], d.grantGroupCommand[2]);

        // Bind commands

        groupCreateCommand.bind("onCreateGroup");
        groupAddCommand.bind("onAddToGroup");
        groupRemoveCommand.bind("onRemoveFromGroup");
        denyPlayerCommand.bind("onDenyPlayer");
        denyGroupCommand.bind("onDenyGroupr");
        grantPlayerCommand.bind("onGrantPlayer");
        grantGroupCommand.bind("onGrantGroup");
    }

    public boolean onAddToGroup(CommandSender messageOutput, String[] parameters)
    {
        if (parameters.length < 2)
        {
            return false;
        }

        String playerName = parameters[0];
        String groupName = parameters[1];

        // First check for group
        Group group = persistence.get(groupName, Group.class);
        if (group == null)
        {
            groupNotFoundMessage.sendTo(messageOutput, groupName);
            return true;
        }

        // Check for player data
        PlayerData user = persistence.get(playerName, PlayerData.class);
        if (user == null)
        {
            playerNotFoundMessage.sendTo(messageOutput, playerName);
            return true;
        }

        user.addToGroup(group);
        persistence.put(user);
        addedPlayerToGroupMessage.sendTo(messageOutput, playerName, groupName);

        return true;
    }

    public boolean onCreateGroup(CommandSender messageOutput,
            String[] parameters)
    {
        if (parameters.length == 0)
        {
            return false;
        }

        String groupName = parameters[0];

        // First check for existing
        Group group = persistence.get(groupName, Group.class);
        if (group != null)
        {
            groupExistsMessage.sendTo(messageOutput, groupName);
            return true;
        }

        group = new Group(groupName);
        persistence.put(group);
        createdGroupMessage.sendTo(messageOutput, groupName);

        return true;
    }

    public boolean onDenyGroup(CommandSender messageOutput, String[] parameters)
    {
        if (parameters.length < 2)
        {
            return false;
        }

        String groupName = parameters[0];
        String profileName = parameters[1];

        // First check for permission profile
        ProfileData profileData = persistence.get(profileName, ProfileData.class);
        if (profileData == null)
        {
            unknownProfileMessage.sendTo(messageOutput, profileName);
            return true;
        }

        // Check for group
        Group group = persistence.get(groupName, Group.class);
        if (group == null)
        {
            groupNotFoundMessage.sendTo(messageOutput, groupName);
            return true;
        }

        group.denyPermission(profileData);
        persistence.put(group);
        denyAccessMessage.sendTo(messageOutput, profileName, "group", groupName);

        return true;
    }

    public boolean onDenyPlayer(CommandSender messageOutput, String[] parameters)
    {
        if (parameters.length < 2)
        {
            return false;
        }

        String playerName = parameters[0];
        String profileName = parameters[1];

        // First check for permission profile
        ProfileData profileData = persistence.get(profileName, ProfileData.class);
        if (profileData == null)
        {
            unknownProfileMessage.sendTo(messageOutput, profileName);
            return true;
        }

        // Check for player data
        PlayerData user = persistence.get(playerName, PlayerData.class);
        if (user == null)
        {
            playerNotFoundMessage.sendTo(messageOutput, playerName);
            return true;
        }

        user.denyPermission(profileData);
        persistence.put(user);
        denyAccessMessage.sendTo(messageOutput, profileName, "player", playerName);

        return true;
    }

    public boolean onGrantGroup(CommandSender messageOutput, String[] parameters)
    {
        if (parameters.length < 2)
        {
            return false;
        }

        String groupName = parameters[0];
        String profileName = parameters[1];

        // First check for permission profile
        ProfileData profileData = persistence.get(profileName, ProfileData.class);
        if (profileData == null)
        {
            unknownProfileMessage.sendTo(messageOutput, profileName);
            return true;
        }

        // Check for group
        Group group = persistence.get(groupName, Group.class);
        if (group == null)
        {
            groupNotFoundMessage.sendTo(messageOutput, groupName);
            return true;
        }

        group.grantPermission(profileData);
        persistence.put(group);
        grantAccessMessage.sendTo(messageOutput, profileName, "group", groupName);

        return true;
    }

    public boolean onGrantPlayer(CommandSender messageOutput,
            String[] parameters)
    {
        if (parameters.length < 2)
        {
            return false;
        }

        String playerName = parameters[0];
        String profileName = parameters[1];

        // First check for permission profile
        ProfileData profileData = persistence.get(profileName, ProfileData.class);
        if (profileData == null)
        {
            unknownProfileMessage.sendTo(messageOutput, profileName);
            return true;
        }

        // Check for player data
        PlayerData user = persistence.get(playerName, PlayerData.class);
        if (user == null)
        {
            playerNotFoundMessage.sendTo(messageOutput, playerName);
            return true;
        }

        user.grantPermission(profileData);
        persistence.put(user);
        grantAccessMessage.sendTo(messageOutput, profileName, "player", playerName);

        return true;
    }

    // TODO: Less copy+paste! In a hurry....
    public boolean onRemoveFromGroup(CommandSender messageOutput,
            String[] parameters)
    {
        if (parameters.length < 2)
        {
            return false;
        }

        String playerName = parameters[0];
        String groupName = parameters[1];

        // First check for group
        Group group = persistence.get(groupName, Group.class);
        if (group == null)
        {
            groupNotFoundMessage.sendTo(messageOutput, group);
            return true;
        }

        // Check for player data
        PlayerData user = persistence.get(playerName, PlayerData.class);
        if (user == null)
        {
            playerNotFoundMessage.sendTo(messageOutput, playerName);
            return true;
        }

        user.removeFromGroup(group);
        persistence.put(user);
        removedPlayerFromGroupMessage.sendTo(messageOutput, playerName, groupName);

        return true;
    }
}
