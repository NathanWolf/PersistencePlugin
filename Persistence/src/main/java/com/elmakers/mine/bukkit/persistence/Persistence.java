package com.elmakers.mine.bukkit.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Server;

import com.elmakers.mine.bukkit.data.DataStore;
import com.elmakers.mine.bukkit.data.DataStoreProvider;
import com.elmakers.mine.bukkit.persisted.EntityInfo;
import com.elmakers.mine.bukkit.persisted.Migrate;
import com.elmakers.mine.bukkit.persisted.MigrationInfo;
import com.elmakers.mine.bukkit.persisted.PersistClass;
import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;

/**
 * The main Persistence interface.
 * 
 * This class is a singleton- use Persistence.getInstance or
 * PersistencePlugin.getPersistence to retrieve the instance.
 * 
 * @author NathanWolf
 */
public class Persistence implements
        com.elmakers.mine.bukkit.persisted.Persistence
{
    private static boolean      allowOpsSUAccess = true;
    // Make sure that we don't create a persisted class twice at the same time
    private static final Object classCreateLock  = new Object();

    private static final Logger log              = Logger.getLogger("Minecraft");

    /**
     * Retrieve the Logger that Persistence uses for debug messages and errors.
     * 
     * Currently, this is hard-coded to the Minecraft server logger.
     * 
     * @return A Logger that can be used for errors or debugging.
     */
    public static Logger getLogger()
    {
        return log;
    }

    public static boolean getOpsCanSU()
    {
        return allowOpsSUAccess;
    }

    public static void setOpsCanSU(boolean allow)
    {
        allowOpsSUAccess = allow;
    }

    // Locks for manual synchronization

    private final Map<Class<? extends Object>, PersistentClass> persistedClassMap = new ConcurrentHashMap<Class<? extends Object>, PersistentClass>();

    private final DataStoreProvider                             provider;

    private final Map<String, Schema>                           schemaMap         = new ConcurrentHashMap<String, Schema>();

    private final Server                                        server;

    /**
     * Persistence is a singleton, so we hide the constructor.
     * 
     * Use PersistencePlugin.getInstance to retrieve a reference to Persistence
     * safely.
     * 
     * @see PersistencePlugin#getPersistence()
     * @see Persistence#getInstance()
     */
    public Persistence(Server server, DataStoreProvider provider)
    {
        this.server = server;
        this.provider = provider;
    }

    /**
     * Clear all data.
     * 
     * This is currently the method used to clear the cache and reload data,
     * however it is flawed. It will probably be replaced with a "reload" method
     * eventually.
     */
    public void clear()
    {
        persistedClassMap.clear();
        schemaMap.clear();
    }

    protected PersistentClass createPersistedClass(Class<? extends Object> persistType, EntityInfo entityInfo) throws InvalidPersistedClassException
    {
        PersistentClass persistedClass = new PersistentClass(this, entityInfo);
        if (!persistedClass.bind(persistType))
        {
            return null;
        }
        String schemaName = persistedClass.getSchemaName();
        Schema schema = getSchema(schemaName);
        if (schema == null)
        {
            schema = createSchema(schemaName);
        }
        schema.addPersistedClass(persistedClass);
        persistedClass.setSchema(schema);

        persistedClassMap.put(persistType, persistedClass);

        // Deferred bind refernces- to avoid circular reference issues
        persistedClass.bindReferences();

        return persistedClass;
    }

    protected Schema createSchema(String schemaName)
    {
        Schema schema = schemaMap.get(schemaName);
        if (schema == null)
        {
            schemaName = schemaName.toLowerCase();
            DataStore store = createStore(schemaName);
            schema = new Schema(schemaName, store);
            schemaMap.put(schemaName, schema);
        }
        return schema;
    }

    protected DataStore createStore(String schema)
    {
        return provider.createStore(schema);
    }

    public void disconnect()
    {
        for (Schema schema : schemaMap.values())
        {
            schema.disconnect();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.elmakers.mine.bukkit.persistence.IPersistence#get(java.lang.Object,
     * java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Object id, Class<T> objectType)
    {
        PersistentClass persistedClass = null;
        try
        {
            persistedClass = getPersistedClass(objectType);
        }
        catch (InvalidPersistedClassException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (persistedClass == null)
        {
            return null;
        }

        Object result = persistedClass.get(id);
        if (result == null)
        {
            return null;
        }
        return (T) result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.elmakers.mine.bukkit.persistence.IPersistence#getAll(java.util.List,
     * java.lang.Class)
     */
    public <T> void getAll(List<T> objects, Class<T> objectType)
    {
        PersistentClass persistedClass = null;
        try
        {
            persistedClass = getPersistedClass(objectType);
        }
        catch (InvalidPersistedClassException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (persistedClass == null)
        {
            return;
        }

        persistedClass.getAll(objects);
    }
    
    public <T> List<T> getAll(Class<T> objectType)
    {
        PersistentClass persistedClass = null;
        try
        {
            persistedClass = getPersistedClass(objectType);
        }
        catch (InvalidPersistedClassException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (persistedClass == null)
        {
            return null;
        }
        List<T> list = new ArrayList<T>();
        persistedClass.getAll(list);
        return list;
    }

    /**
     * Retrieve or create a persisted class, using the annotations built into
     * the class.
     * 
     * @param persistClass
     *            The annotated Class to persist
     * @return The persisted class definition, or null if failure
     */
    public PersistentClass getPersistedClass(Class<? extends Object> persistClass) throws InvalidPersistedClassException
    {
        /*
         * Look for Class annotations
         */

        // TODO: Lookup from schema/name map ... hm... uh, how to do this
        // without the annotation?
        // I guess pass in one, and then other persisted classes can request
        // data from their own schema...
        PersistentClass persistedClass = persistedClassMap.get(persistClass);
        if (persistedClass == null)
        {
            PersistClass entityAnnotation = persistClass.getAnnotation(PersistClass.class);
            Migrate migrationAnnotation = persistClass.getAnnotation(Migrate.class);

            if (entityAnnotation == null)
            {
                throw new InvalidPersistedClassException(persistClass, "Class does not have the @PersistClass annotation");
            }

            persistedClass = getPersistedClass(persistClass, new EntityInfo(entityAnnotation));

            if (migrationAnnotation != null)
            {
                persistedClass.setMigrationInfo(new MigrationInfo(persistedClass, migrationAnnotation));
            }

        }

        return persistedClass;
    }

    /**
     * Retrieve or create a persisted class definition for a given class type.
     * 
     * This can be used to create a persisted class based on a existing class.
     * 
     * @param persistType
     *            The Class to persist
     * @param entityInfo
     *            Information on how to persist this class
     * @return The persisted class definition, or null if failure
     */
    public PersistentClass getPersistedClass(Class<? extends Object> persistType, EntityInfo entityInfo)
    {
        PersistentClass persistedClass = persistedClassMap.get(persistType);
        if (persistedClass == null)
        {
            // Lock now, to create an atomic checkCreate for class:
            synchronized (classCreateLock)
            {
                persistedClass = persistedClassMap.get(persistType);
                if (persistedClass == null)
                {
                    try
                    {
                        persistedClass = createPersistedClass(persistType, entityInfo);
                    }
                    catch (InvalidPersistedClassException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        return persistedClass;
    }

    /**
     * Retrieve a Schema definition, with a list of PersistedClasses.
     * 
     * This function is used for inspecting schemas and entities.
     * 
     * @param schemaName
     *            The schema to retrieve
     * @return A Schema definition class, containing entity classes
     */
    public Schema getSchema(String schemaName)
    {
        return schemaMap.get(schemaName);
    }

    /**
     * Retrieve a list of definitions for all known schemas.
     * 
     * This function is used for inspecting schemas and entities.
     * 
     * @return The list of schemas
     */
    public List<Schema> getSchemaList()
    {
        List<Schema> schemaList = new ArrayList<Schema>();
        schemaList.addAll(schemaMap.values());
        return schemaList;
    }

    /*
     * Protected members
     */

    public Server getServer()
    {
        return server;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.elmakers.mine.bukkit.persistence.IPersistence#put(java.lang.Object)
     */
    public boolean put(Object persist)
    {
        if (persist == null)
        {
            return false;
        }

        PersistentClass persistedClass = null;
        try
        {
            persistedClass = getPersistedClass(persist.getClass());
        }
        catch (InvalidPersistedClassException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (persistedClass == null)
        {
            return false;
        }

        persistedClass.put(persist);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.elmakers.mine.bukkit.persistence.IPersistence#putAll(java.util.List,
     * java.lang.Class)
     */
    public <T> void putAll(List<T> objects, Class<T> objectType)
    {
        PersistentClass persistedClass = null;
        try
        {
            persistedClass = getPersistedClass(objectType);
        }
        catch (InvalidPersistedClassException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (persistedClass == null)
        {
            return;
        }

        persistedClass.putAll(objects);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.elmakers.mine.bukkit.persistence.IPersistence#remove(java.lang.Object
     * )
     */
    public void remove(Object removeObject)
    {
        PersistentClass persistedClass = null;
        try
        {
            persistedClass = getPersistedClass(removeObject.getClass());
        }
        catch (InvalidPersistedClassException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (persistedClass == null)
        {
            return;
        }

        persistedClass.remove(removeObject);
    }

    /**
     * Force a save of all cached data.
     * 
     * This only saves dirty data- unmodified data is not saved back to the
     * database. Persistence calls save() internally on server shutdown, player
     * login, and player logout. So, calling save is not mandatory- you only
     * need to use it to force an immediate save.
     * 
     */
    public void save()
    {
        for (PersistentClass persistedClass : persistedClassMap.values())
        {
            persistedClass.save();
        }
    }
}
