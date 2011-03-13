package com.elmakers.mine.bukkit.permission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.elmakers.mine.bukkit.permission.dao.ProfileData;
import com.elmakers.mine.bukkit.persisted.Persistence;
import com.elmakers.mine.craftbukkit.permission.InvalidPermissionProfileException;
import com.elmakers.mine.craftbukkit.permission.PermissionDescriptionException;
import com.elmakers.mine.craftbukkit.permission.PermissionDescriptionNode;
import com.elmakers.mine.craftbukkit.permission.PermissionDescriptionNodeException;
import com.elmakers.mine.craftbukkit.permission.PermissionProfile;
import com.elmakers.mine.craftbukkit.permission.RootPermissionDescription;

public class PermissionManager implements PermissionHandler
{
	public PermissionManager(Server server, Persistence persistence)
	{
		this.server = server;
		this.persistence = persistence;
	}
	
	public void loadPluginPermissions(Plugin plugin)
	{
		File dataFolder = plugin.getDataFolder();
		String pluginName = dataFolder.getName();
		File file = new File(dataFolder.getParentFile(), pluginName + ".jar");
		try
		{
			if (!file.exists())
			{
				throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
			}
			try
			{
				JarFile jar = new JarFile(file);
				JarEntry entry = jar.getJarEntry("plugin.yml");

				if (entry == null)
				{
					throw new InvalidPluginException(new FileNotFoundException("Jar does not contain plugin.yml"));
				}

				InputStream stream = jar.getInputStream(entry);
				
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>)yaml.load(stream);
				if (map.containsKey("permissions"))
				{
					try
					{
						@SuppressWarnings("unchecked")
						Map<String, Object> perms = (Map<String, Object>)map.get("permissions");
						
						RootPermissionDescription rootNode = new RootPermissionDescription(perms);
						addPluginRootPermission(pluginName, rootNode);
					}
					catch (ClassCastException ex)
					{
						throw new InvalidDescriptionException(ex, "permissions are of wrong type");
					}
					catch (PermissionDescriptionException ex)
					{
						throw new InvalidDescriptionException(ex, "permissions are invalid");
					}
					catch (PermissionDescriptionNodeException ex)
					{
						throw new InvalidDescriptionException(ex, "permissions are invalid");
					}
				}

				stream.close();
				jar.close();
			}
			catch (IOException ex)
			{
				throw new InvalidPluginException(ex);
			}
		}
		catch (Throwable ex)
		{
			log.log(Level.INFO, "Error reading plugin permissions: ", ex);
		}
	}
	
	public boolean isSet(Player player, String permissionNode)
	{
		if (defaultProfile != null)
		{
			if (defaultProfile.isSet(permissionNode))
			{
				return true;
			}
		}

		for (RootPermissionDescription rootNodes : permissions.values())
		{
			if (rootNodes.isDefaultSet(permissionNode))
			{
				return true;
			}
		}
		for (PermissionHandler subHandler : handlers)
		{
			if (subHandler.isSet(player, permissionNode))
			{
				return true;
			}
		}
		return false;
	}
	
	public void addPluginRootPermission(String pluginName, RootPermissionDescription rootNode)
	{
		if (permissions.get(pluginName) != null) return;
	       	
		if (rootNode != null)
		{
			String[] names = rootNode.getNames();
			for (String name : names)
			{
				permissions.put(name, rootNode);
			}
		}
	}
	
	public void loadPermissions(File permissionsFile)
	{		
		// Set up player profiles for permissions
		FileReader loader = null;
		try
		{
			loader = new FileReader(permissionsFile);

			if (!loadProfiles(loader, permissionsFile))
			{
				log.info("Persistence: There's an error with permissions.yml - hopefully more info about that above.");
			}
		}
		catch(FileNotFoundException ex)
		{
			log.info("Persistence: Create a plugins/Persistence/" + permissionsFile + " to use internal permissions");
			loader = null;
		}
	}

	protected boolean loadProfiles(Reader reader, File permissionsFile)
	{
		PermissionProfile[] profiles;
		try
		{
			profiles = PermissionProfile.loadProfiles(this, server, reader);
			log.info("Persistence: loaded " + profiles.length + " profiles from " + permissionsFile.getName());
			for (PermissionProfile profile : profiles)
			{
				String profileName = profile.getName();
				if (profileName.equalsIgnoreCase("default"))
				{
					defaultProfile = profile;
				}
				ProfileData profileData = persistence.get(profileName, ProfileData.class);
				if (profileData == null)
				{
					profileData = new ProfileData(profileName);
					persistence.put(profileData);
				}
				
				/// This is setting a transient instance
				profileData.setProfile(profile);
			}
		}
		catch (InvalidPermissionProfileException e)
		{
			log.info(e.getMessage());
			return false;
		}
		
		return true;
	}
	
	public RootPermissionDescription getPermissionRoot(final String path)
	{
		String root = path.split("\\.", 2)[0];
		return permissions.get(root);
	}

	public PermissionDescriptionNode getPermissionPath(final String path)
	{
		RootPermissionDescription root = getPermissionRoot(path);

		/*
		 * TODO: Add a path cache to avoid having to keep searching for nodes It
		 * will be much more efficient. Need to invalidate the cache every time
		 * a plugin changes one of the node descriptions though (not that they
		 * should...)
		 */

		if (root == null)
		{
			throw new IllegalArgumentException("No permissions are defined for " + path);
		}

		return root.getPath(path);
	}
	
	public void addHandler(PermissionHandler handler)
	{
		handlers.add(handler);
	}
	
	protected final Server									server;
	protected final Persistence								persistence;

	private final List<PermissionHandler>					handlers		= new ArrayList<PermissionHandler>();
	private final Map<String, RootPermissionDescription>	permissions		= new HashMap<String, RootPermissionDescription>();
	protected PermissionProfile								defaultProfile	= null;

	private static final Yaml								yaml			= new Yaml(new SafeConstructor());
	protected static final Logger							log				= Logger.getLogger("Minecraft");
}
