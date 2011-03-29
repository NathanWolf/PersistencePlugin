package com.elmakers.mine.bukkit.borrowed;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityTypes;
import net.minecraft.server.WorldServer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Creature;

/**
 * Yeah, I stole this- couldn't wait for giants. Sorry!
 * 
 */
public enum CreatureType
{
    UNKNOWN("Unknown"),
    CHICKEN("Chicken"),
    COW("Cow"),
    CREEPER("Creeper"),
    GHAST("Ghast"),
    GIANT("Giant"),
    PIG("Pig"),
    PIG_ZOMBIE("PigZombie"),
    SHEEP("Sheep"),
    SKELETON("Skeleton"),
    SLIME("Slime"),
    SPIDER("Spider"),
    SQUID("Squid"),
    ZOMBIE("Zombie");

    private String name;
    private static final Map<String, CreatureType> mapping = new HashMap<String, CreatureType>();

    static
    {
        for (CreatureType type : EnumSet.allOf(CreatureType.class))
        {
            mapping.put(type.name, type);
        }
    }

    public static CreatureType fromName(String name)
    {
        return mapping.get(name);
    }

    private CreatureType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
    
    public Creature spawn(Location loc) 
    {
        Creature creature;
        try 
        {
            World w = loc.getWorld();
            CraftWorld cw = (CraftWorld)w;
            WorldServer world = cw.getHandle();
            CraftServer server = world.getServer();

            EntityCreature entityCreature = (EntityCreature) EntityTypes.a(getName(), world);
            entityCreature.a(loc.getX(), loc.getY(), loc.getZ());

            creature = (Creature)CraftEntity.getEntity(server, entityCreature);
            world.a(entityCreature);
        } 
        catch (Exception e) 
        {
            // if we fail, for any reason, return null.
            creature = null;
        }
        return creature;
    }

}
