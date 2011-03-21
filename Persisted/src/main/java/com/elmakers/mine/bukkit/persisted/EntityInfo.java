package com.elmakers.mine.bukkit.persisted;

public class EntityInfo
{
    private boolean cached    = true;

    private boolean contained = false;

    private String  name;

    private String  schema;

    public EntityInfo(PersistClass defaults)
    {
        schema = defaults.schema();
        name = defaults.name();
        contained = defaults.contained();
        cached = defaults.cached();
    }

    public EntityInfo(String schema, String name)
    {
        this.schema = schema;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getSchema()
    {
        return schema;
    }

    public boolean isCached()
    {
        return cached;
    }

    public boolean isContained()
    {
        return contained;
    }

    public void setCached(boolean cached)
    {
        this.cached = cached;
    }

    public void setContained(boolean contained)
    {
        this.contained = contained;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }
}
