package com.elmakers.mine.bukkit.persistence.dao;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.BlockVector;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

@PersistClass(schema = "global", name = "world")
public class WorldData extends Persisted
{
    protected Environment environmentType;

    protected long        id;

    protected String      name;

    protected BlockVector spawn;

    protected World       world;

    public WorldData()
    {

    }

    public WorldData(String name, Environment type)
    {
        this.name = name;
        setEnvironmentType(type);
    }

    public WorldData(World world)
    {
        update(world);
    }

    @PersistField
    public Environment getEnvironmentType()
    {
        return environmentType;
    }

    @PersistField(id = true)
    public String getName()
    {
        return name;
    }

    @PersistField(contained = true)
    public BlockVector getSpawn()
    {
        return spawn;
    }

    public World getWorld()
    {
        if (world != null)
        {
            return world;
        }
        if (persistedClass == null)
        {
            return null;
        }

        Server server = persistedClass.getServer();
        if (server == null)
        {
            return null;
        }

        List<World> worlds = server.getWorlds();
        for (World checkWorld : worlds)
        {
            if (checkWorld.getName().equalsIgnoreCase(name))
            {
                this.world = checkWorld;
                return world;
            }
        }

        world = server.createWorld(name, getEnvironmentType());
        return world;
    }

    public void setEnvironmentType(Environment environmentType)
    {
        this.environmentType = environmentType;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSpawn(BlockVector spawn)
    {
        this.spawn = spawn;
    }

    public void update(World world)
    {
        this.world = world;

        name = world.getName();
        id = world.getId();
        Location location = world.getSpawnLocation();
        spawn = new BlockVector(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        setEnvironmentType(world.getEnvironment());
    }
}
