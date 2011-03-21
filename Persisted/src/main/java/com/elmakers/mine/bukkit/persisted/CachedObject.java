package com.elmakers.mine.bukkit.persisted;

public class CachedObject
{
    private boolean cached;
    private long    cacheTime;
    private boolean dirty;
    private Object  object;

    public CachedObject(Object o)
    {
        object = o;
        cached = true;
        dirty = false;
        cacheTime = System.currentTimeMillis();
        updateCacheTime();
    }

    public long getCacheTime()
    {
        return cacheTime;
    }

    public Object getObject()
    {
        return object;
    }

    public boolean isCached()
    {
        return cached;
    }

    public boolean isDirty()
    {
        return dirty;
    }

    public void setCached(boolean c)
    {
        cached = c;
    }

    public void setObject(Object o)
    {
        object = o;
        dirty = true;
        updateCacheTime();
    }

    public void setSaved()
    {
        dirty = false;
        updateCacheTime();
    }

    protected void updateCacheTime()
    {
        cacheTime = System.currentTimeMillis();
    }

}
