package com.elmakers.mine.bukkit.persistence.dao;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.Persisted;

@PersistClass(schema="global", name="block")
public class BlockData extends Persisted
{
	public BlockData()
	{
		
	}
	
	public BlockData(Block block)
	{
		this.block = block;
		
		location = new LocationData(block.getWorld(), block.getX(), block.getY(), block.getZ());
		material = block.getType();
		materialData = block.getData();
	}
	
	public BlockData(BlockData copy)
	{
		location = copy.location;
		material = copy.material;
		materialData = copy.materialData;
		block = copy.block;
	}
	
	@PersistField(contained=true, id=true)
	public LocationData getLocation()
	{
		return location;
	}

	public void setLocation(LocationData location)
	{
		this.location = location;
	}

	@PersistField
	public Material getMaterial()
	{
		return material;
	}

	public void setMaterial(Material material)
	{
		this.material = material;
	}

	@PersistField
	public byte getMaterialData()
	{
		return materialData;
	}

	public void setMaterialData(byte materialData)
	{
		this.materialData = materialData;
	}

	public Block getBlock()
	{
		if (block == null && location != null)
		{
			Location blockLocation = location.getLocation();
			if (blockLocation != null)
			{
				block = blockLocation.getWorld().getBlockAt(blockLocation);
			}
		}
		return block;
	}
	
	protected boolean checkBlock()
	{
		if (block == null)
		{
			block = getBlock();
		}
		
		return (block != null);
	}
	
	public boolean undo()
	{
		if (!checkBlock()) return false;
		
		World world = block.getWorld();
		Chunk chunk = world.getChunkAt(block);
		if (!world.isChunkLoaded(chunk)) return false;
		
		if (block.getType() != material || block.getData() != materialData)
		{
			block.setType(material);
			block.setData(materialData);
		}
		
		return true;
	}
	
	public static long getBlockId(Block block)
	{
		return (block.getWorld().getName().hashCode() << 28) ^
		 (Integer.valueOf(block.getX()).hashCode() << 13) ^
         (Integer.valueOf(block.getY()).hashCode() << 7) ^
          Integer.valueOf(block.getZ()).hashCode();
	}
	
	public static long getBlockId(BlockData blockData)
	{
		 return getBlockId(blockData.getBlock());
	}
	
	public static BlockFace getReverseFace(BlockFace blockFace)
	{
		switch (blockFace)
		{
			case NORTH: return BlockFace.SOUTH;
			case WEST: return BlockFace.EAST;
			case SOUTH: return BlockFace.NORTH;
			case EAST: return BlockFace.WEST;
			case UP: return BlockFace.DOWN;
			case DOWN: return BlockFace.UP;
		}
		
		return BlockFace.SELF;
	}

	public LocationData location;
	public Material		material;
	public byte			materialData;
	
	public static final BlockFace[] FACES = new BlockFace[] {BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN};
	public static final BlockFace[] SIDES = new BlockFace[] {BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST};
	
	// Transient
	protected Block block;
}
