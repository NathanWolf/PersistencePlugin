package com.elmakers.mine.bukkit.persistence.dao;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

/**
 * Represents a possible command sender.
 * 
 * This entity is pre-populated, currently only "generic" and "player" present.
 * 
 * Use of this class is currently hard-coded, so it would not be advised to add
 * or modify this data.
 * 
 * @author nathan
 * 
 */
@PersistClass(name = "sender", schema = "global")
public class CommandSenderData extends Persisted
{
    protected String className;

    protected String id;

    public CommandSenderData()
    {

    }

    public CommandSenderData(String id, Class<?> senderClass)
    {
        this.id = id;
        if (senderClass != null)
        {
            this.className = senderClass.getName();
        }
    }

    @PersistField
    public String getClassName()
    {
        return className;
    }

    @PersistField(id = true)
    public String getId()
    {
        return id;
    }

    public Class<?> getType()
    {
        if (className == null || className.length() == 0)
        {
            return null;
        }
        Class<?> senderType = null;
        try
        {
            senderType = Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            senderType = null;
        }
        return senderType;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
