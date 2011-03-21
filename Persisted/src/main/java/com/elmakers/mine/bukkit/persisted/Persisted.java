package com.elmakers.mine.bukkit.persisted;

import org.bukkit.Server;

import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;

/**
 * This is an (optional) common base class for all persisted classes.
 * 
 * It implements Cloneable for you, and also provides easy access to the
 * PersistedClass that manages its instances.
 * 
 * @author NathanWolf
 */
public class Persisted implements Cloneable
{
    // TODO: Support one Persistence instance per server
    protected static Persistence persistence = null;

    public static void setPersistence(Server server, Persistence persistence)
    {
        Persisted.persistence = persistence;
    }

    protected PersistedClass persistedClass = null;

    public Persisted()
    {
        try
        {
            persistedClass = persistence.getPersistedClass(getClass());
            if (persistedClass == null)
            {
                throw new InvalidPersistedClassException(getClass(), "Failed to initialize");
            }
        }
        catch (InvalidPersistedClassException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Automatically generate a copy of this instance based on its persistend
     * data.
     * 
     * @return a copy of this persisted object
     */
    @Override
    public Persisted clone()
    {
        // TODO!
        return null;
    }

    public Persistence getPersistence()
    {
        return persistedClass.getPersistence();
    }

    /**
     * Generate a hash id based on the hash id of this object's concrete (data)
     * id.
     * 
     * @return an auto-generated hash code
     */
    @Override
    public int hashCode()
    {
        if (persistedClass == null)
        {
            return 0;
        }
        Object id = persistedClass.getIdData(this);
        if (id == null)
        {
            return 0;
        }

        return id.hashCode();
    }
}
