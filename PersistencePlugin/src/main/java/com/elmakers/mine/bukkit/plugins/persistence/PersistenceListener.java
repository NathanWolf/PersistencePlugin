package com.elmakers.mine.bukkit.plugins.persistence;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.elmakers.mine.bukkit.persistence.Persistence;
import com.elmakers.mine.bukkit.persistence.dao.PlayerData;

public class PersistenceListener extends PlayerListener
{
    private PersistenceCommands commands;

    private Persistence         persistence;

    public void initialize(Persistence persistence, PersistenceCommands commands)
    {
        this.persistence = persistence;
        this.commands = commands;
    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        // TODO!
        // PersistencePlugin.getInstance().getPermissions().initializePermissions();

        Player player = event.getPlayer();
        String playerName = player.getName();
        PlayerData playerData = persistence.get(playerName, PlayerData.class);
        if (playerData == null)
        {
            playerData = new PlayerData(player);
        }
        playerData.login(player);
        if (!commands.getSUCommand().checkPermission(player))
        {
            playerData.setSuperUser(false);
        }
        persistence.put(playerData);
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String playerName = player.getName();
        PlayerData playerData = persistence.get(playerName, PlayerData.class);
        if (playerData != null)
        {
            playerData.disconnect(player);
            persistence.put(playerData);
        }
    }
}
