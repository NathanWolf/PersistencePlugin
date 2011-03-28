package com.elmakers.mine.bukkit.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Server;

import com.elmakers.com.bukkit.data.exception.InvalidDataException;
import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataRow;
import com.elmakers.mine.bukkit.data.DataStore;
import com.elmakers.mine.bukkit.data.DataTable;
import com.elmakers.mine.bukkit.data.DataType;
import com.elmakers.mine.bukkit.persisted.CachedObject;
import com.elmakers.mine.bukkit.persisted.EntityInfo;
import com.elmakers.mine.bukkit.persisted.FieldInfo;
import com.elmakers.mine.bukkit.persisted.MigrationInfo;
import com.elmakers.mine.bukkit.persisted.PersistField;
import com.elmakers.mine.bukkit.persisted.PersistedClass;
import com.elmakers.mine.bukkit.persisted.PersistedReference;
import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;

/**
 * Represents and manages a single persisted class.
 * 
 * This class binds to a Class, looking for @Persist and @PersistClass tags.
 * 
 * It will keep track of the persisted fields of a class, and handle creating
 * and caching object instances of that class.
 * 
 * @author NathanWolf
 * 
 */
public class PersistentClass implements PersistedClass
{
    enum LoadState
    {
        LOADED, LOADING, UNLOADED,
    }

    protected static Logger             log             = Persistence.getLogger();
    protected Map<Object, CachedObject> cacheMap        = new ConcurrentHashMap<Object, CachedObject>();

    protected boolean                   cacheObjects    = false;
    protected Map<Object, CachedObject> concreteIdMap   = new ConcurrentHashMap<Object, CachedObject>();

    protected boolean                   contained       = false;
    protected PersistedField            container       = null;
    protected boolean                   dirty           = false;

    protected EntityInfo                entityInfo      = null;

    protected List<PersistedList>       externalFields  = new ArrayList<PersistedList>();
    // TODO: Make sure these are ok non-concurrent? Should never be writing to
    // these after startup!
    protected List<PersistedField>      fields          = new ArrayList<PersistedField>();
    protected PersistedField            idField         = null;
    protected List<PersistedField>      internalFields  = new ArrayList<PersistedField>();

    protected LoadState                 loadState       = LoadState.UNLOADED;
    protected long                      maxId           = 1;
    protected MigrationInfo             migrationInfo   = null;

    protected String                    name            = null;
    protected Class<? extends Object>   persistClass    = null;

    protected final Persistence         persistence;
    protected List<PersistedReference>  referenceFields = new ArrayList<PersistedReference>();
    protected Map<Object, CachedObject> removedMap      = new ConcurrentHashMap<Object, CachedObject>();

    protected Schema                    schema          = null;

    protected String                    schemaName      = null;

    public PersistentClass(Persistence persistence, EntityInfo entityInfo)
    {
        this.entityInfo = entityInfo;
        this.persistence = persistence;
    }

    public PersistentClass(PersistentClass copy, PersistedField container) throws InvalidPersistedClassException
    {
        this.persistence = copy.persistence;
        this.contained = true;
        this.schema = copy.schema;
        this.container = container;
        this.name = copy.name;
        this.schemaName = copy.schemaName;
        this.entityInfo = copy.entityInfo;
        this.persistClass = copy.persistClass;

        // TODO: Make sure it's ok to share fields!
        for (PersistedField field : copy.fields)
        {
            // If a field is an id field, we don't care about it in the
            // container, unless the container is also the id field
            // Or, if the container is a list, we have to store the id.
            // TODO: Consider this logic for validity (already faulty, feeling
            // too complex now)
            if (!field.isIdField() || container.isIdField() || container instanceof PersistedList)
            {
                addField(field.clone(), field.getFieldInfo());
            }
        }
    }

    public boolean addField(PersistedField field, FieldInfo fieldInfo) throws InvalidPersistedClassException
    {
        if (fieldInfo.isIdField())
        {
            if (idField != null)
            {
                throw new InvalidPersistedClassException(this, "Can't have more than one id field");
            }
            idField = field;
        }

        if (field instanceof PersistedList)
        {
            PersistedList list = (PersistedList) field;
            if (list.getListDataType() == DataType.LIST)
            {
                throw new InvalidPersistedClassException(this, "Lists of lists not supported");
            }
            externalFields.add(list);
            if (list.isObject())
            {
                referenceFields.add(list);
            }
        }
        else if (field instanceof PersistedObject)
        {
            PersistedObject reference = (PersistedObject) field;
            internalFields.add(reference);
            if (reference.isObject())
            {
                referenceFields.add(reference);
            }
        }
        else
        {
            if (fieldInfo.isContained())
            {
                throw new InvalidPersistedClassException(this, "Only List and Object fields may be contained");
            }
            if (fieldInfo.isAutogenerated())
            {
                if (!fieldInfo.isIdField())
                {
                    throw new InvalidPersistedClassException(this, "Only id fields may be autogenerated");
                }
                else if (!field.getType().isAssignableFrom(Integer.class) && !field.getType().isAssignableFrom(int.class))
                {
                    throw new InvalidPersistedClassException(this, "Only integer fields may be autogenerated");
                }
            }
            internalFields.add(field);
        }

        field.setContainer(container);

        fields.add(field);

        return true;
    }

    protected CachedObject addToCache(Object o)
    {
        return addToCache(o, null);
    }

    protected CachedObject addToCache(Object o, Object concreteId)
    {
        // Handle autogen ids
        // 0 (and lower) are "magic numbers" signifying that there is no auto
        // id set, and we need to generate one.
        boolean autogenerate = idField.isAutogenerated();
        Object id = null;
        if (!autogenerate)
        {
            id = getId(o);
            if (id != null)
            {
                // Check to see if this object has already been removed, if so
                // un-remove it
                CachedObject removedObject = removedMap.get(id);
                if (removedObject != null)
                {
                    removedMap.remove(id);
                }
            }
            if (concreteId == null)
            {
                concreteId = getIdData(o);
            }
        }
        else
        {
            // TODO: This is not the place for this- find a better place to set
            // up the auto id!
            // Addendum: maybe? I don't like the magic number thing, but cache
            // add _is_ kind
            // of the place for this..

            id = getId(o);
            Long intValue = null;

            // Think I'm going to recommend, but not _enforce_ using a Long for
            // an auto int id...
            boolean usingLong = id instanceof Long;

            if (usingLong)
            {
                intValue = (Long) id;
            }
            else
            {
                intValue = (Long) DataType.convertValue(id, Long.class);
            }
            if (intValue == null || intValue <= 0)
            {
                intValue = maxId++;
                if (usingLong)
                {
                    id = intValue;
                }
                else
                {
                    id = DataType.convertValue(intValue, idField.getType());
                }
            }
            concreteId = id;
            try
            {
                idField.set(o, id);
            }
            catch (InvalidDataException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (id == null && concreteId == null)
        {
            return null;
        }

        CachedObject cached = new CachedObject(o);
        if (id != null)
        {
            cacheMap.put(id, cached);
        }
        if (concreteId != null)
        {
            concreteIdMap.put(concreteId, cached);
        }

        return cached;
    }

    public boolean bind(Class<? extends Object> persistClass) throws InvalidPersistedClassException
    {
        this.persistClass = persistClass;

        cacheObjects = entityInfo.isCached();
        schemaName = entityInfo.getSchema();
        name = entityInfo.getName();

        name = name.replace(" ", "_");
        schemaName = schemaName.replace(" ", "_");
        schema = null; // Persistence will assign a schema after binding

        if (!cacheObjects)
        {
            throw new InvalidPersistedClassException(this, "Non-cached objects no supported, yet");
        }

        /*
         * Find fields, getters and setters
         */

        idField = null;

        for (Field classField : persistClass.getDeclaredFields())
        {
            PersistField persist = classField.getAnnotation(PersistField.class);
            if (persist != null)
            {
                PersistedField field = PersistedField.tryCreate(new FieldInfo(persist), classField, this);
                if (field == null)
                {
                    throw new InvalidPersistedClassException(this, "Field " + persistClass.getName() + "." + classField.getName() + " is not persistable, type=" + classField.getType().getName());
                }
                else
                {
                    addField(field, new FieldInfo(persist));
                }
            }
        }

        for (Method method : persistClass.getDeclaredMethods())
        {
            PersistField persist = method.getAnnotation(PersistField.class);
            if (persist != null)
            {
                PersistedField field = PersistedField.tryCreate(new FieldInfo(persist), method, this);
                if (field == null)
                {
                    Class<?> type = method.getReturnType();
                    if (type == void.class && method.getParameterTypes().length > 0)
                    {
                        type = method.getParameterTypes()[0];
                    }
                    throw new InvalidPersistedClassException(this, "Field " + persistClass.getName() + "." + method.getName() + " is not persistable, type=" + type.getName());
                }
                else
                {
                    addField(field, new FieldInfo(persist));
                }
            }
        }

        return true;
    }

    public void bindReferences()
    {
        for (PersistedField field : fields)
        {
            try
            {
                field.bind();
            }
            catch (InvalidPersistedClassException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected void checkLoadCache()
    {
        try
        {
            checkLoadCache(getDefaultStore());
        }
        catch (InvalidDataException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void checkLoadCache(DataStore store) throws InvalidDataException
    {
        if (loadState == LoadState.UNLOADED && cacheObjects)
        {
            loadState = LoadState.LOADING;
            try
            {
                if (store.connect())
                {
                    validateTables(store);
                    loadCache();
                    loadState = LoadState.LOADED;
                }
            }
            catch (Throwable e)
            {
                clear();
                throw new InvalidDataException(e);
            }
        }
    }

    public void clear()
    {
        cacheMap.clear();
        concreteIdMap.clear();
        loadState = LoadState.UNLOADED;
    }

    protected Object createInstance(DataRow row) throws InvalidDataException
    {
        Object newObject = null;

        try
        {
            newObject = persistClass.newInstance();
            load(row, newObject);
        }
        catch (IllegalAccessException ex)
        {
            throw new InvalidDataException(row.getTable(), row, ex);
        }
        catch (InstantiationException ex)
        {
            throw new InvalidDataException(row.getTable(), row, ex);
        }
        return newObject;
    }

    public Object get(Object id)
    {
        checkLoadCache();
        PersistedField idField = getIdField();
        if (idField == null)
        {
            return null;
        }

        Object result = getById(idField, cacheMap, id);
        if (result == null)
        {
            idField = idField.getConcreteField();
            result = getById(idField, concreteIdMap, id);
        }

        return result;
    }

    public Object get(Object id, Object defaultValue)
    {
        checkLoadCache();
        Object result = get(id);
        if (result != null)
        {
            return result;
        }

        if (defaultValue == null)
        {
            return null;
        }

        CachedObject cached = addToCache(defaultValue);
        return cached.getObject();
    }

    @SuppressWarnings("unchecked")
    public <T> void getAll(List<T> objects)
    {
        checkLoadCache();
        for (CachedObject cachedObject : cacheMap.values())
        {
            Object object = cachedObject.getObject();
            if (persistClass.isAssignableFrom(object.getClass()))
            {
                objects.add((T) object);
            }
        }
    }

    protected Object getById(PersistedField idField, Map<Object, CachedObject> fromCache, Object id)
    {
        if (idField == null || id == null)
        {
            return null;
        }
        if (id.getClass().isAssignableFrom(idField.getType()))
        {
            CachedObject cached = fromCache.get(id);
            if (cached != null)
            {
                return cached.getObject();
            }
        }
        else
        {
            // Try to do some fancy casting.
            // This is mainly here to avoid the Integer/int problem.

            DataField requestId = new DataField(id);
            id = requestId.getValue(idField.getType());

            CachedObject cached = fromCache.get(id);
            if (cached != null)
            {
                return cached.getObject();
            }
        }

        return null;
    }

    protected DataTable getClassTable()
    {
        DataTable classTable = new DataTable(getTableName());
        return classTable;
    }

    public PersistedField getConcreteIdField()
    {
        if (idField == null)
        {
            return null;
        }
        return idField.getConcreteField();
    }

    public String getContainedIdName()
    {
        String idName = getTableName();
        if (idField != null)
        {
            idName = PersistedField.getContainedName(idName, idField.getDataName());
        }
        return idName;
    }

    public String getContainedIdName(PersistedField container)
    {
        String idName = container.getDataName();
        if (idField != null)
        {
            idName = PersistedField.getContainedName(idName, idField.getDataName());
        }
        return idName;
    }

    public DataStore getDefaultStore()
    {
        if (schema == null)
        {
            return null;
        }
        return schema.getStore();
    }

    public EntityInfo getEntityInfo()
    {
        return entityInfo;
    }

    public int getFieldCount()
    {
        return fields.size();
    }

    public Object getId(Object o)
    {
        Object value = null;
        if (idField != null)
        {
            value = idField.get(o);
        }
        return value;
    }

    /**
     * getIdData will recurse down objects-as-id reference chains. This is for
     * persisting in the data store.
     * 
     * getId will return the actual id value, which is how data is cached
     * internally.
     * 
     * A VERY important distinction! You look up object-as-id objects using
     * their id instance, not the id of their id (of that id's id, etc...)
     */
    public Object getIdData(Object o)
    {
        Object value = null;
        PersistedField field = idField;
        if (field != null)
        {
            if (field instanceof PersistedReference)
            {
                PersistedReference ref = (PersistedReference) field;
                Object refId = idField.get(o);
                if (ref.getReferenceType() != null)
                {
                    return ref.getReferenceType().getIdData(refId);
                }
            }
            else
            {
                value = idField.get(o);
            }
        }
        return value;
    }

    public PersistedField getIdField()
    {
        return idField;
    }

    protected DataTable getListTable(PersistedList list)
    {
        DataTable listTable = new DataTable(list.getTableName());
        return listTable;
    }

    public MigrationInfo getMigrationInfo()
    {
        return migrationInfo;
    }

    public String getName()
    {
        return name;
    }

    public List<PersistedField> getPersistedFields()
    {
        return fields;
    }

    public com.elmakers.mine.bukkit.persisted.Persistence getPersistence()
    {
        return persistence;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public Server getServer()
    {
        return persistence.getServer();
    }

    public String getTableName()
    {
        return name;
    }

    public Class<? extends Object> getType()
    {
        return persistClass;
    }

    public boolean hasContainer()
    {
        return container != null;
    }

    public boolean isContainedClass()
    {
        return entityInfo.isContained();
    }

    public boolean isDirty()
    {
        return dirty;
    }

    /*
     * Protected members
     */

    public void load(DataRow row, Object o) throws InvalidDataException
    {
        for (PersistedField field : internalFields)
        {
            if (field.isReadOnly())
            {
                continue;
            }

            field.load(row, o);
        }
    }

    protected void loadCache() throws InvalidDataException
    {
        loadCache(getDefaultStore());
    }

    protected void loadCache(DataStore store) throws InvalidDataException
    {
        if (!store.connect())
        {
            return;
        }

        DataTable classTable = getClassTable();
        store.load(classTable);

        // Begin deferred referencing, to prevent the problem of DAO's
        // referencing unloaded DAOs.
        // DAOs will be loaded recursively as needed,
        // and then all deferred references will be resolved afterward.
        PersistedObject.beginDefer();

        for (DataRow row : classTable.getRows())
        {
            Object newInstance = createInstance(row);

            if (newInstance != null)
            {
                if (idField.isAutogenerated())
                {
                    int id = (Integer) idField.get(newInstance);
                    if (id > maxId)
                    {
                        maxId = id;
                    }
                }

                // cache by concrete (data) is from the store
                // as well as the actual id
                // This covers the case of "object as id", when that
                // Object may not be loaded yet
                DataField idData = row.get(idField.getDataName());
                Object concreteId = idData.getValue();

                addToCache(newInstance, concreteId);
            }
        }

        // Bind deferred references, to handle DAOs referencing other DAOs, even
        // of the
        // Same type.
        // DAOs will be loaded recursively as needed, and then references bound
        // when everything has been
        // resolved.
        PersistedObject.endDefer();

        // Defer load lists of entities
        PersistedList.beginDefer();

        // Load list data
        if (externalFields.size() > 0)
        {
            List<Object> instances = new ArrayList<Object>();
            for (CachedObject cached : cacheMap.values())
            {
                instances.add(cached.getObject());
            }
            for (PersistedList list : externalFields)
            {
                DataTable listTable = getListTable(list);
                store.load(listTable);
                list.load(listTable, instances);
            }
        }

        // Load any reference lists
        PersistedList.endDefer();
    }

    protected void logMigrateError(String schema, String table)
    {
        log.warning("Persistence: Can't migrate entity " + schema + "." + table);
        log.warning("             If you continue to have issues, please delete the table " + table + " in the " + schema + " database");
    }

    /**
     * Will create a table if it does not exist, and migrate data as necessary
     * if it does exist.
     * 
     * @param table
     *            The table definition. If this differs from the stored
     *            definition, data migration will occur.
     * @return true if success
     * @see #tableExists(DataTable)
     */
    public boolean migrateEntity(DataStore store, DataTable table)
    {
        if (!store.tableExists(table.getName()))
        {
            store.create(table);
            return true;
        }

        // Migrate data
        DataTable currentTable = store.getTableHeader(table.getName());
        DataRow tableHeader = table.getHeader();
        DataRow currentHeader = currentTable.getHeader();
        if (tableHeader.isMigrationRequired(currentHeader))
        {
            MigrationInfo migrateInfo = getMigrationInfo();

            // TODO: Support types other than auto reset
            if (migrateInfo == null)
            {
                log.info("Persistence: Auto-migrating entity " + getSchema() + "." + getName());

                /*
                 * TODO! String autoBackupTable = table.getName() +
                 * "_autoBackup"; if (tableExists(autoBackupTable)) {
                 * drop(autoBackupTable); }
                 * currentTable.setName(autoBackupTable); create(currentTable);
                 */
                store.drop(currentTable.getName());
                store.create(table);
            }
            else
            {
                // Custom migration not supported- just dump error.
                logMigrateError(getSchemaName(), getName());
            }
        }

        return true;
    }

    /*
     * TODO: make this check for duplicate fields, and also maybe rename to
     * getPersistedField
     */
    public PersistedField persistField(String fieldName) throws InvalidPersistedClassException
    {
        return persistField(fieldName, new FieldInfo());
    }

    public PersistedField persistField(String fieldName, FieldInfo fieldInfo) throws InvalidPersistedClassException
    {
        PersistedField persistField = null;
        String getterName = fieldName;
        if (fieldInfo.getGetter() != null && fieldInfo.getGetter().length() > 0)
        {
            getterName = fieldInfo.getGetter();
        }
        Method getter = PersistedField.findGetter(getterName, persistClass);

        if (getter != null)
        {
            persistField = PersistedField.tryCreate(fieldInfo, getter, this);
        }
        else
        {
            if (fieldInfo.getField() != null && fieldInfo.getField().length() > 0)
            {
                fieldName = fieldInfo.getField();
            }
            Field field = PersistedField.findField(fieldName, persistClass);
            if (field != null)
            {
                persistField = PersistedField.tryCreate(fieldInfo, field, this);
            }
        }
        if (persistField != null)
        {
            addField(persistField, fieldInfo);
        }
        return persistField;
    }

    public void populate(DataRow row, Object instance)
    {
        for (PersistedField field : internalFields)
        {
            try
            {
                field.save(row, instance);
            }
            catch (InvalidDataException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected void populate(DataTable dataTable, Collection<CachedObject> instances)
    {
        for (CachedObject instance : instances)
        {
            DataRow instanceRow = new DataRow(dataTable);
            populate(instanceRow, instance.getObject());
            dataTable.addRow(instanceRow);
        }
    }

    public void populateHeader(DataTable table)
    {
        for (PersistedField field : internalFields)
        {
            field.populateHeader(table);
        }
    }

    public void put(Object o)
    {
        checkLoadCache();

        Object id = getId(o);
        CachedObject co = cacheMap.get(id);
        if (co == null)
        {
            co = addToCache(o);
        }

        // TODO: merge
        co.setCached(cacheObjects);
        co.setObject(o);
        dirty = true;
    }

    public void putAll(List<? extends Object> objects)
    {
        checkLoadCache();
        // TODO: merge...
    }

    public void remove(Object o)
    {
        Object id = getId(o);
        removeFromCache(id);
        dirty = true;
    }

    protected void removeFromCache(Object id)
    {
        CachedObject co = cacheMap.get(id);
        if (co == null)
        {
            return;
        }

        if (cacheMap.containsKey(id))
        {
            cacheMap.remove(id);
        }
        if (concreteIdMap.containsKey(id))
        {
            concreteIdMap.remove(id);
        }
        removedMap.put(id, co);
    }

    public void reset()
    {
        reset(getDefaultStore());
    }

    public void reset(DataStore store)
    {
        if (!store.connect())
        {
            return;
        }

        DataTable resetTable = getClassTable();
        store.drop(resetTable.getName());

        // Reset any list sub-tables
        for (PersistedList list : externalFields)
        {
            DataTable listTable = getListTable(list);
            store.drop(listTable.getName());
        }

        maxId = 1;
    }

    public void save()
    {
        save(getDefaultStore());
    }

    public void save(DataStore store)
    {
        if (loadState != LoadState.LOADED)
        {
            return;
        }
        if (!dirty)
        {
            return;
        }

        // Drop removed objects
        Collection<CachedObject> removedList = removedMap.values();
        if (removedList.size() > 0)
        {
            DataTable clearTable = getClassTable();
            populate(clearTable, removedList);
            store.clear(clearTable);

            removedMap.clear();
        }

        // Save dirty objects
        List<CachedObject> dirtyObjects = new ArrayList<CachedObject>();
        for (CachedObject cached : cacheMap.values())
        {
            if (cached.isDirty())
            {
                dirtyObjects.add(cached);
            }
        }

        save(dirtyObjects, store);
        dirty = false;
    }

    public void save(List<CachedObject> instances)
    {
        save(instances, getDefaultStore());
    }

    public void save(List<CachedObject> instances, DataStore store)
    {
        if (!store.connect())
        {
            return;
        }

        // Save main class data
        DataTable classTable = getClassTable();
        populate(classTable, instances);
        store.save(classTable);

        // Save list data
        for (PersistedList list : externalFields)
        {
            DataTable listTable = getListTable(list);
            List<Object> instanceIds = new ArrayList<Object>();

            for (CachedObject instance : instances)
            {
                Object id = getIdData(instance.getObject());
                instanceIds.add(id);
                list.save(listTable, instance.getObject());
            }

            // First, delete removed items
            store.clearIds(listTable, instanceIds);

            // Save new list data
            store.save(listTable);
        }

        for (CachedObject cached : instances)
        {
            cached.setSaved();
        }
    }

    public void setMigrationInfo(MigrationInfo migrationInfo)
    {
        this.migrationInfo = migrationInfo;
    }

    public void setSchema(Schema schema)
    {
        this.schema = schema;
    }

    public boolean validate() throws InvalidPersistedClassException
    {
        if (idField == null && !isContainedClass())
        {
            throw new InvalidPersistedClassException(this, "Must specify one id field. Use an auto int if you need.");
        }

        if (fields.size() == 0)
        {
            throw new InvalidPersistedClassException(this, "Persisted class has no persisted fields.");
        }

        for (PersistedReference reference : referenceFields)
        {
            if (reference.getReferenceType() == null)
            {
                throw new InvalidPersistedClassException(this, "Field " + reference.getName() + " references a non-persistable class: " + reference.getType().getName());
            }
        }

        return true;
    }

    /*
     * Private data
     */

    protected void validateTables(DataStore store)
    {
        if (!store.connect())
        {
            return;
        }
        DataTable classTable = getClassTable();

        classTable.createHeader();
        populateHeader(classTable);

        migrateEntity(store, classTable);

        // Validate any list sub-tables
        for (PersistedList list : externalFields)
        {
            DataTable listTable = getListTable(list);
            listTable.createHeader();
            list.populateHeader(listTable);
            migrateEntity(store, listTable);
        }
    }
}
