package com.elmakers.mine.bukkit.persisted;

import java.util.ArrayList;
import java.util.List;

import com.elmakers.mine.bukkit.persistence.dao.MigrationStep;

public class MigrationInfo
{
	public MigrationInfo()
	{
	}
	
	public MigrationInfo(PersistedClass entityClass, Migrate info)
	{
		if (info.steps() != null)
		{
			steps = new ArrayList<MigrationStep>();
			for (MigrateStep stepInfo : info.steps())
			{
				MigrationStep step = new MigrationStep(entityClass, stepInfo);
				steps.add(step);
			}
		}
	}

	public List<MigrationStep> getSteps()
	{
		return steps;
	}

	protected List<MigrationStep>	steps	= null;
}
