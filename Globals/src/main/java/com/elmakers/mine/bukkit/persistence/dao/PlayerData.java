package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.elmakers.mine.bukkit.permission.PermissionHandler;
import com.elmakers.mine.bukkit.permission.dao.ProfileData;
import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

/**
 * Encapsulate a player in a persitable class.
 * 
 * You can use this class in your own data objects to reference a player,
 * instead of using playerName.
 * 
 * The player name is used an id, so it is still what will ultimately get
 * persisted to your data table.
 * 
 * TOOD: common base class between this in PlayerGroup, once i know that won't
 * break persistence of these objects.
 * 
 * @author NathanWolf
 * 
 */

@PersistClass(name = "player", schema = "global")
public class PlayerData extends Persisted
{
    private static PermissionHandler permissions = null;

    public static void setPermissionHandler(PermissionHandler permissions)
    {
        PlayerData.permissions = permissions;
    }

    private List<ProfileData>            deny;

    private HashMap<String, ProfileData> denyMap;

    private Date                         firstLogin;

    private List<ProfileData>            grant;

    private HashMap<String, ProfileData> grantMap;

    private HashMap<String, Group>       groupMap;

    private List<Group>                  groups;

    private String                       id;

    private Date                         lastDisconnect;

    private Date                         lastLogin;

    private LocationData                 location;

    private String                       name;

    private boolean                      online;

    // Transient - will be set up by the groups manager
    private Player                       player;

    private boolean                      superUser;

    /**
     * The default constructor, used by Persistence to create new instances.
     */
    public PlayerData()
    {
    }

    /**
     * Create a new instance based on a logged in Player.
     * 
     * Sets the first login time to now, and sets the id from the player name.
     * 
     * If the player is an Op, "superUser" is set to true by default, though
     * they will have the ability to turn this on and off.
     * 
     * @param loggedIn
     *            the player this data will represent
     */
    public PlayerData(Player loggedIn)
    {
        id = loggedIn.getName();
        firstLogin = new Date();
        lastDisconnect = null;
        superUser = false;
        update(loggedIn);
    }

    public void addToGroup(Group group)
    {
        if (groupMap == null)
        {
            groupMap = new HashMap<String, Group>();
        }
        if (groups == null)
        {
            groups = new ArrayList<Group>();
        }

        if (groupMap.get(group.getId()) == null)
        {
            groupMap.put(group.getId(), group);
            groups.add(group);
        }
    }

    public void denyPermission(ProfileData profile)
    {
        if (denyMap == null)
        {
            denyMap = new HashMap<String, ProfileData>();
        }
        if (deny == null)
        {
            deny = new ArrayList<ProfileData>();
        }

        if (denyMap.get(profile.getId()) == null)
        {
            denyMap.put(profile.getId(), profile);
            deny.add(profile);
        }

        // Remove from the allow map if present, since we'd block it anyway.
        if (grantMap != null)
        {
            ProfileData allowProfile = grantMap.get(profile.getId());
            if (allowProfile != null)
            {
                grantMap.remove(allowProfile.getId());
                if (deny != null)
                {
                    grant.remove(allowProfile);
                }
            }
        }
    }

    /**
     * Update this data based on a player disconnecting.
     * 
     * Will update online status and last disconnect time.
     * 
     * @param player
     *            The player that logged out.
     */
    public void disconnect(Player player)
    {
        online = false;
        lastDisconnect = new Date();
    }

    @PersistField
    public List<ProfileData> getDeny()
    {
        return deny;
    }

    @PersistField
    public Date getFirstLogin()
    {
        return firstLogin;
    }

    @PersistField
    public List<ProfileData> getGrant()
    {
        return grant;
    }

    @PersistField
    public List<Group> getGroups()
    {
        return groups;
    }

    @PersistField(id = true)
    public String getId()
    {
        return id;
    }

    @PersistField
    public Date getLastDisconnect()
    {
        return lastDisconnect;
    }

    @PersistField
    public Date getLastLogin()
    {
        return lastLogin;
    }

    public LocationData getLocation()
    {
        return location;
    }

    @PersistField
    public String getName()
    {
        return name;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void grantPermission(ProfileData profile)
    {
        if (grantMap == null)
        {
            grantMap = new HashMap<String, ProfileData>();
        }
        if (grant == null)
        {
            grant = new ArrayList<ProfileData>();
        }

        if (grantMap.get(profile.getId()) == null)
        {
            grantMap.put(profile.getId(), profile);
            grant.add(profile);
        }

        // Now, make sure to remove from the deny map also
        // This is more for inherited permissions, we don't
        // want to block ourselves here.
        if (denyMap != null)
        {
            ProfileData denyProfile = denyMap.get(profile.getId());
            if (denyProfile != null)
            {
                denyMap.remove(denyProfile.getId());
                if (deny != null)
                {
                    deny.remove(denyProfile);
                }
            }
        }
    }

    @PersistField
    public boolean isOnline()
    {
        return online;
    }

    public boolean isSet(String key)
    {
        if (superUser)
        {
            return true;
        }

        // Check for deny first
        if (deny != null)
        {
            for (ProfileData profile : deny)
            {
                if (profile.isSet(key))
                {
                    return false;
                }
            }
        }

        // Check grant
        if (grant != null)
        {
            for (ProfileData profile : grant)
            {
                if (profile.isSet(key))
                {
                    return true;
                }
            }
        }

        // Check groups
        if (groups != null)
        {
            for (Group group : groups)
            {
                if (group.isSet(key))
                {
                    return true;
                }
            }
        }

        if (permissions != null)
        {
            return permissions.isSet(player, key);
        }

        return false;
    }

    public boolean isSuperUser()
    {
        return superUser;
    }

    public void login(Player player)
    {
        lastLogin = new Date();
        update(player);
    }

    public void removeFromGroup(Group group)
    {
        if (groupMap == null || groups == null)
        {
            return;
        }

        Group storedGroup = groupMap.get(group.getId());
        if (group != null)
        {
            groups.remove(storedGroup);
            groupMap.remove(storedGroup.getId());
        }
    }

    public void setDeny(List<ProfileData> deny)
    {
        this.deny = deny;

        denyMap = new HashMap<String, ProfileData>();
        for (ProfileData profile : deny)
        {
            denyMap.put(profile.getId(), profile);
        }
    }

    public void setFirstLogin(Date firstLogin)
    {
        this.firstLogin = firstLogin;
    }

    public void setGrant(List<ProfileData> grant)
    {
        this.grant = grant;

        grantMap = new HashMap<String, ProfileData>();
        for (ProfileData profile : grant)
        {
            grantMap.put(profile.getId(), profile);
        }
    }

    public void setGroups(List<Group> groups)
    {
        this.groups = groups;

        groupMap = new HashMap<String, Group>();
        for (Group group : groups)
        {
            groupMap.put(group.getId(), group);
        }
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setLastDisconnect(Date lastDisconnect)
    {
        this.lastDisconnect = lastDisconnect;
    }

    public void setLastLogin(Date lastLogin)
    {
        this.lastLogin = lastLogin;
    }

    @PersistField(contained = true)
    public void setLocation(LocationData location)
    {
        this.location = location;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setOnline(boolean online)
    {
        this.online = online;
    }

    @PersistField
    public void setSuperUser(boolean su)
    {
        this.superUser = su;
    }

    public void update(Location location)
    {
        this.location = new LocationData(location);
    }

    /**
     * Update data based on a logged-in player.
     * 
     * Will update online status, display name, and last login time.
     * 
     * @param player
     *            The player to use when updating this data.
     */

    public void update(Player player)
    {
        this.player = player;
        update(player.getLocation());
        name = player.getDisplayName();
        online = player.isOnline();
    }
}
