package com.elmakers.mine.bukkit.gameplay;

import org.bukkit.block.Block;

import com.elmakers.mine.bukkit.gameplay.dao.BlockList;

public interface BlockRecurseAction
{
	public boolean perform(Block block);
	public void setAffectedBlocks(BlockList blocks);
}
