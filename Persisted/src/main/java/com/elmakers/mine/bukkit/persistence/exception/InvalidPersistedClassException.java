package com.elmakers.mine.bukkit.persistence.exception;

import com.elmakers.mine.bukkit.persisted.EntityInfo;
import com.elmakers.mine.bukkit.persisted.PersistedClass;

public class InvalidPersistedClassException extends Exception
{
    /**
     * Need to support Serializable via Exception
     */
    private static final long       serialVersionUID = 1L;

    private EntityInfo              entityInfo       = null;

    private PersistedClass          persistedClass   = null;

    private Class<? extends Object> persistedType    = null;

    public InvalidPersistedClassException(Class<? extends Object> persistedType)
    {
        this.persistedType = persistedType;
    }

    public InvalidPersistedClassException(
            Class<? extends Object> persistedType, String message)
    {
        super(message);
        this.persistedType = persistedType;
    }

    public InvalidPersistedClassException(EntityInfo entityInfo)
    {
        this.entityInfo = entityInfo;
    }

    public InvalidPersistedClassException(EntityInfo entityInfo, String message)
    {
        super(message);
        this.entityInfo = entityInfo;
    }

    public InvalidPersistedClassException(PersistedClass persistedClass)
    {
        this.persistedClass = persistedClass;
        if (persistedClass != null)
        {
            this.entityInfo = persistedClass.getEntityInfo();
            this.persistedType = persistedClass.getType();
        }
    }

    public InvalidPersistedClassException(PersistedClass persistedClass,
            String message)
    {
        super(message);
        this.persistedClass = persistedClass;
        if (persistedClass != null)
        {
            this.entityInfo = persistedClass.getEntityInfo();
            this.persistedType = persistedClass.getType();
        }
    }

    public EntityInfo getEntityInfo()
    {
        return entityInfo;
    }

    public PersistedClass getPersistedClass()
    {
        return persistedClass;
    }

    public Class<? extends Object> getPersistedType()
    {
        return persistedType;
    }
}
