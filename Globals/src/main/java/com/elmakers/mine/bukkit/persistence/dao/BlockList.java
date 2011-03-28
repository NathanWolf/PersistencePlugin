package com.elmakers.mine.bukkit.persistence.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;

/**
 * 
 * Implements a Collection of Blocks, for quick getting/putting while iterating
 * over a set or area of blocks.
 * 
 * This stores BlockData objects, which are hashable via their Persisted
 * inheritence, and their LocationData id (which itself has a hash function
 * based on world name and BlockVector's hash function)
 * 
 * @author NathanWolf
 * 
 */
@PersistClass(schema = "global", name = "blockList", contained = true)
public class BlockList implements Collection<BlockData>
{
    /**
     * Default serial id, in case you want to serialize this (probably shouldn't
     * though!)
     * 
     * Persist it instead, once I've got that working.
     */
    private static final long      serialVersionUID = 1L;

    protected BoundingBox          area;

    protected HashSet<Long>        blockIdMap;

    // HashMap backing and easy persistence - need an extra list for this right
    // now.
    protected ArrayList<BlockData> blockList;
    protected HashSet<BlockData>   blockMap;

    protected int                  passesRemaining  = 1;
    protected int                  timeRemaining    = 0;
    protected int                  timeToLive       = 0;

    public BlockList()
    {

    }

    public BlockList(BlockList other)
    {
        for (BlockData block : other)
        {
            BlockData newBlock = new BlockData(block);
            add(newBlock);
        }
        timeToLive = other.timeToLive;
        timeRemaining = other.timeRemaining;
        passesRemaining = other.passesRemaining;
    }

    public boolean add(Block block)
    {
        if (contains(block))
        {
            return true;
        }

        BlockData newBlock = new BlockData(block);
        return add(newBlock);
    }

    public boolean add(BlockData blockData)
    {
        // First do a sanity check with the map
        // Currently, we don't replace blocks!
        if (blockMap != null)
        {
            if (blockMap.contains(blockData))
            {
                return true;
            }
        }
        else
        {
            blockMap = new HashSet<BlockData>();
        }

        if (blockIdMap == null)
        {
            blockIdMap = new HashSet<Long>();
        }

        if (blockList == null)
        {
            blockList = new ArrayList<BlockData>();
        }
        BlockVector blockLocation = blockData.getLocation().getPosition();

        if (area == null)
        {
            area = new BoundingBox(blockLocation, blockLocation);
        }
        else
        {
            area = area.contain(blockLocation);
        }

        blockMap.add(blockData);
        blockIdMap.add(BlockData.getBlockId(blockData));
        return blockList.add(blockData);
    }

    public boolean addAll(Collection<? extends BlockData> blocks)
    {
        // Iterate to maintain BB area
        boolean added = true;
        for (BlockData block : blocks)
        {
            added = added && add(block);
        }
        return added;
    }

    public boolean age(int t)
    {
        boolean triggered = false;
        timeRemaining -= t;
        if (isExpired())
        {
            passesRemaining--;
            if (passesRemaining > 0)
            {
                timeRemaining = timeToLive;
            }
            triggered = true;
        }
        return triggered;
    }

    public void clear()
    {
        if (blockList == null)
        {
            return;
        }
        blockList.clear();
    }

    public boolean contains(Block block)
    {
        if (blockMap == null)
        {
            return false;
        }

        return blockIdMap.contains(BlockData.getBlockId(block));
    }

    public boolean contains(BlockData blockData)
    {
        if (blockMap == null || blockData == null)
        {
            return false;
        }

        return blockMap.contains(blockData);
    }

    public boolean contains(Object arg0)
    {
        // Fall back to map
        return blockMap.contains(arg0) || blockIdMap.contains(arg0);
    }

    public boolean containsAll(Collection<?> arg0)
    {
        if (blockMap == null)
        {
            return false;
        }
        return blockMap.containsAll(arg0);
    }

    // Collection interface- would be great if I could just extend HashSet and
    // have this "just work"
    // TODO : Make that happen in Persistence!

    // For now, this is here to keep the map up to date, and to pass through to
    // the blockList.

    @PersistField(contained = true)
    public BoundingBox getArea()
    {
        return area;
    }

    @PersistField(name="blocks")
    public ArrayList<BlockData> getBlockList()
    {
        return blockList;
    }

    public void setBlockList(ArrayList<BlockData> blockList)
    {
        this.blockList = blockList;
        if (blockList != null)
        {
            blockMap = new HashSet<BlockData>();
            blockIdMap = new HashSet<Long>();
            for (BlockData block : blockList)
            {
                blockMap.add(block);
                blockIdMap.add(BlockData.getBlockId(block));
            }
        }
    }
    
    public boolean isEmpty()
    {
        if (blockList == null)
        {
            return true;
        }
        return blockList.isEmpty();
    }

    public boolean isExpired()
    {
        return timeRemaining <= 0;
    }

    public Iterator<BlockData> iterator()
    {
        if (blockList == null)
        {
            return null;
        }
        return blockList.iterator();
    }

    public boolean remove(Object arg0)
    {
        // Note that we never shrink the BB!
        if (blockList == null)
        {
            return false;
        }
        return blockList.remove(arg0);
    }

    public boolean removeAll(Collection<?> arg0)
    {
        if (blockList == null)
        {
            return false;
        }
        return blockList.removeAll(arg0);
    }

    public boolean retainAll(Collection<?> arg0)
    {
        if (blockList == null)
        {
            return false;
        }
        return blockList.retainAll(arg0);
    }

    public void setArea(BoundingBox area)
    {
        this.area = area;
    }

    public void setRepetitions(int repeat)
    {
        passesRemaining = repeat;
    }

    public void setTimeToLive(int ttl)
    {
        timeToLive = ttl;
        timeRemaining = ttl;
    }

    public int size()
    {
        if (blockList == null)
        {
            return 0;
        }
        return blockList.size();
    }

    public Object[] toArray()
    {
        if (blockList == null)
        {
            return null;
        }
        return blockList.toArray();
    }

    public <T> T[] toArray(T[] arg0)
    {
        if (blockList == null)
        {
            return null;
        }
        return blockList.toArray(arg0);
    }

    public boolean undo()
    {
        for (BlockData block : this)
        {
            if (!block.undo())
            {
                return false;
            }
        }

        return true;
    }
}
