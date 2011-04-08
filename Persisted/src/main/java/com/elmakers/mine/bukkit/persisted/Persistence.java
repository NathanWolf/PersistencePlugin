package com.elmakers.mine.bukkit.persisted;

import java.util.List;

import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;

public interface Persistence
{

    /**
     * Retrieve an instance of the specified type.
     * 
     * This method retrieves an object instance from the data store, based on
     * the object's id. The id passed in should match the type of this object's
     * id field- a String for a String id, for instance.
     * 
     * If an object with the specified id cannot be found, the method returns
     * null;
     * 
     * @param <T>
     *            The base type of object. This is an invisible parameter, you
     *            don't need to worry about it
     * @param id
     *            The id of the object to lookup
     * @param objectType
     *            The type of object to search for
     * @return The object instance with the specified id, or null if not found
     */
    public abstract <T> T get(Object id, Class<T> objectType);

    /**
     * Populates a list of all instances of a specified type.
     * 
     * This is a parameterized function. It will populate a list with object
     * instances for a given type. An example call:
     * 
     * List<MyObject> myInstances = new ArrayList<MyObject>();
     * persistence.getAll(myInstances, MyObject.class);
     * 
     * This would populate the myInstances list with any persisted MyObject
     * instances. You must ensure that your List is of a compatible type with
     * the objects you are retrieving.
     * 
     * @param <T>
     *            The base type of object. This is an invisible parameter, you
     *            don't need to worry about it
     * @param objects
     *            A List (needs not be empty) to populate with object instances
     * @param objectType
     *            The type of object to retrieve
     */
    public abstract <T> void getAll(List<T> objects, Class<T> objectType);
    
    public abstract <T> List<T> getAll(Class<T> objectType);

    /**
     * Retrieve or create a persisted class, using the annotations built into
     * the class.
     * 
     * @param persistClass
     *            The annotated Class to persist
     * @return The persisted class definition, or null if failure
     */
    public PersistedClass getPersistedClass(Class<? extends Object> persistClass) throws InvalidPersistedClassException;

    /**
     * Add an object to the data store.
     * 
     * This only adds the object to the cache. At save time, the cached object
     * will trigger a data save.
     * 
     * If this is the first instance of this type of object to ever be stored,
     * the schema and tables needed to store this object will be created at save
     * time. Then, the tables will be populated with this object's data.
     * 
     * @param persist
     *            The object to persist
     * @return false if, for some reason, the storage failed.
     */
    public abstract boolean put(Object persist);

    /**
     * Merge a list of objects into the data store.
     * 
     * Use this method to completely replace the stored entity list for a type
     * of entity. Entities not in the "objects" list will be deleted, new
     * objects will be added, and existing objects merged.
     * 
     * This is a parameterized function. It will populate a list with object
     * instances for a given type. An example call:
     * 
     * List<MyObject> myInstances = new ArrayList<MyObject>(); ... Fill
     * myInstances with some data persistence.putAll(myInstances,
     * MyObject.class);
     * 
     * This would replace all instances of MyObject with the instances in the
     * myInstances list.
     * 
     * TODO: Currently, this method replaces all of the instances directly. This
     * would invalidate any externally maintained references, so it needs to
     * merge data instead.
     * 
     * @param <T>
     *            The base type of object. This is an invisible parameter, you
     *            don't need to worry about it
     * @param objects
     *            A list of objects to store
     * @param objectType
     *            The type of object to replace instances
     */
    public abstract <T> void putAll(List<T> objects, Class<T> objectType);

    /**
     * Remove an object from the cache (and data store on save)
     * 
     * @param removeObject
     *            The object to remove
     */
    public abstract void remove(Object removeObject);

}