package com.elmakers.mine.bukkit.persistence.dao;

import org.bukkit.Location;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

@PersistClass(schema = "global", name = "orientation", contained = true)
public class Orientation extends Persisted
{
    protected float pitch;

    protected float yaw;

    public Orientation()
    {

    }

    public Orientation(float yaw, float pitch)
    {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Orientation(Location location)
    {
        yaw = location.getYaw();
        pitch = location.getPitch();
    }

    @PersistField
    public float getPitch()
    {
        return pitch;
    }

    @PersistField
    public float getYaw()
    {
        return yaw;
    }

    public void setPitch(float pitch)
    {
        this.pitch = pitch;
    }

    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }
}
