package com.elmakers.mine.bukkit.gameplay;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import com.elmakers.mine.bukkit.persistence.dao.MaterialList;

public class CSVParser
{
	public static MaterialList parseMaterials(String csvList)
	{
		MaterialList materials = new MaterialList();
		
		String[] matIds = csvList.split(",");
		for (String matId : matIds)
		{
			try
			{
				int typeId = Integer.parseInt(matId.trim());
				materials.add(Material.getMaterial(typeId));
			}
			catch (NumberFormatException ex)
			{
				
			}
		}
		return materials;
	}
	
	public static List<Integer> parseIntegers(String csvList)
	{
		List<Integer> ints = new ArrayList<Integer>();
		
		String[] intStrings = csvList.split(",");
		for (String s : intStrings)
		{
			try
			{
				int thisInt = Integer.parseInt(s.trim());
				ints.add(thisInt);
			}
			catch (NumberFormatException ex)
			{
				
			}
		}
		return ints;
	}
}
