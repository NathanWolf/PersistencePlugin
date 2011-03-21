package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.elmakers.mine.bukkit.permission.dao.ProfileData;
import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;

/**
 * 
 * Represents a group of players
 * 
 * * TOOD: common base class between this in PlayerGroup, once i know that won't
 * break persistence of these objects.
 * 
 * @author NathanWolf
 * 
 */
@PersistClass(schema = "global", name = "group")
public class Group
{
    protected List<ProfileData>          deny;

    private HashMap<String, ProfileData> denyMap;

    protected List<ProfileData>          grant;

    // Transient
    private HashMap<String, ProfileData> grantMap;

    protected String                     id;

    protected Group                      parent;

    public Group()
    {

    }

    public Group(String id)
    {
        this.id = id;
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

    public <T> T get(final String key)
    {
        /*
         * TODO : implement this
         * 
         * T result = null;
         * 
         * // Check for deny first // Any kind of data returned by a deny
         * profile // will get returned as null. for (ProfileData profile :
         * deny) {
         * 
         * }
         */

        return null;
    }

    @PersistField
    public List<ProfileData> getDeny()
    {
        return deny;
    }

    @PersistField
    public List<ProfileData> getGrant()
    {
        return grant;
    }

    @PersistField(id = true)
    public String getId()
    {
        return id;
    }

    @PersistField
    public Group getParent()
    {
        return parent;
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

    public boolean isSet(String key)
    {
        // Check for deny first
        for (ProfileData profile : deny)
        {
            if (profile.isSet(key))
            {
                return false;
            }
        }

        for (ProfileData profile : grant)
        {
            if (profile.isSet(key))
            {
                return true;
            }
        }

        return false;
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

    public void setGrant(List<ProfileData> grant)
    {
        this.grant = grant;

        grantMap = new HashMap<String, ProfileData>();
        for (ProfileData profile : grant)
        {
            grantMap.put(profile.getId(), profile);
        }
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setParent(Group parent)
    {
        this.parent = parent;
    }
}
