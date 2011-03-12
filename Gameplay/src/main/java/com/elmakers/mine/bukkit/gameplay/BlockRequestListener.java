package com.elmakers.mine.bukkit.gameplay;

import java.util.List;

import org.bukkit.block.Block;

public interface BlockRequestListener
{
	public void onBlockListLoaded(List<Block> blocks);
}
