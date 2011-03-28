package com.elmakers.mine.bukkit.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.elmakers.com.bukkit.data.exception.InvalidDataException;
import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataRow;
import com.elmakers.mine.bukkit.data.DataTable;
import com.elmakers.mine.bukkit.data.DataType;
import com.elmakers.mine.bukkit.persisted.FieldInfo;
import com.elmakers.mine.bukkit.persisted.PersistedReference;
import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;

/**
 * A variant of PersistedField that handles persisting Lists
 * 
 * The class tries to abstract some of the complexity of persisting Lists of
 * data, including creating and using sub-tables.
 * 
 * It also supports Lists of contained objects, storing object data directly in
 * the list sub-table.
 * 
 * @author NathanWolf
 * 
 */
public class PersistedList extends PersistedField implements PersistedReference
{
    class DeferredReferenceList
    {
        public List<Object>  idList;
        public PersistedList referenceList;

        public DeferredReferenceList(PersistedList listField)
        {
            referenceList = listField;
        }
    }

    private static final List<PersistedList> deferredLists   = new ArrayList<PersistedList>();
    private static int                       deferStackDepth = 0;

    public static void beginDefer()
    {
        deferStackDepth++;
    }

    public static void endDefer()
    {
        deferStackDepth--;
        if (deferStackDepth > 0)
        {
            return;
        }

        List<PersistedList> lists = new ArrayList<PersistedList>();
        lists.addAll(deferredLists);
        deferredLists.clear();
        for (PersistedList list : lists)
        {
            list.bindDeferredInstances();
        }
    }

    private final HashMap<Object, DeferredReferenceList> deferredInstanceMap = new HashMap<Object, DeferredReferenceList>();

    protected DataType                                   listDataType;

    protected Class<?>                                   listType;

    // Only valid for Lists of Objects
    protected PersistentClass                            referenceType       = null;

    protected String                                     tableName;

    public PersistedList(FieldInfo fieldInfo, Field field, PersistentClass owningClass)
    {
        super(fieldInfo, field, owningClass);
        findListType();
    }

    public PersistedList(FieldInfo fieldInfo, Method getter, Method setter, PersistentClass owningClass)
    {
        super(fieldInfo, getter, setter, owningClass);
        findListType();
    }

    public PersistedList(PersistedList copy)
    {
        super(copy);

        if (isContained())
        {
            try
            {
                referenceType = new PersistentClass(copy.referenceType, this);
            }
            catch (InvalidPersistedClassException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            referenceType = copy.referenceType;
        }
        findListType();
    }

    @Override
    public void bind() throws InvalidPersistedClassException
    {
        if (listDataType == DataType.OBJECT)
        {
            try
            {
                referenceType = owningClass.persistence.getPersistedClass(listType);
            }
            catch (InvalidPersistedClassException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (referenceType == null)
            {
                return;
            }

            if (isContained())
            {
                // Create a sub-class of the reference class
                referenceType = new PersistentClass(referenceType, this);
                referenceType.bindReferences();
            }
            else
            {
                if (referenceType.isContainedClass())
                {
                    log.warning("Persistence: " + owningClass.getSchemaName() + "." + owningClass.getTableName() + "." + getDataName() + ", entity " + referenceType.getTableName() + " must be contained");
                    referenceType = null;
                }
            }
        }
    }

    public void bindDeferredInstances()
    {

        for (Object instance : deferredInstanceMap.keySet())
        {
            List<Object> references = new ArrayList<Object>();
            DeferredReferenceList ref = deferredInstanceMap.get(instance);
            for (Object id : ref.idList)
            {
                if (id == null)
                {
                    references.add(null);
                }
                else
                {
                    Object reference = ref.referenceList.referenceType.get(id);
                    references.add(reference);
                }
            }

            try
            {
                ref.referenceList.set(instance, references);
            }
            catch (InvalidDataException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        deferredInstanceMap.clear();
    }

    @Override
    public PersistedList clone()
    {
        PersistedList field = new PersistedList(this);
        return field;
    }

    protected void findListType()
    {
        Type type = getGenericType();

        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getActualTypeArguments().length > 0)
            {
                listType = (Class<?>) pt.getActualTypeArguments()[0];
            }
        }
        listDataType = DataType.getTypeFromClass(listType);

        // Construct sub-table name
        tableName = name.substring(0, 1).toUpperCase() + name.substring(1);
        tableName = owningClass.getTableName() + tableName;
    }

    protected Type getGenericType()
    {
        Type genericType = null;
        if (getter != null)
        {
            genericType = getter.getGenericReturnType();
        }
        else
        {
            genericType = field.getGenericType();
        }
        return genericType;
    }

    public DataType getListDataType()
    {
        return listDataType;
    }

    public Class<?> getListType()
    {
        return listType;
    }

    public String getReferenceIdName()
    {
        if (referenceType == null)
        {
            return null;
        }

        // Construct a field name using the name of the reference id
        return referenceType.getContainedIdName(this);
    }

    @Override
    public PersistentClass getReferenceType()
    {
        return referenceType;
    }

    public String getTableName()
    {
        return tableName;
    }

    // PersistedReference interface
    public boolean isObject()
    {
        return listDataType == DataType.OBJECT;
    }

    public void load(DataTable subTable, List<Object> instances) throws InvalidDataException
    {
        load(subTable, instances, null);
    }

    public void load(DataTable subTable, List<Object> instances, PersistedField container) throws InvalidDataException
    {
        // Load data for all lists in all instances at once, mapping to
        // correct instances based on the id column.

        // Map ids to their objects
        HashMap<Object, Object> objectIdMap = new HashMap<Object, Object>();

        // Maintain a list of object ids to their lists of object instances
        HashMap<Object, List<Object>> objectLists = new HashMap<Object, List<Object>>();
        for (Object instance : instances)
        {
            Object instanceId = owningClass.getIdData(instance);
            objectIdMap.put(instanceId, instance);
            List<Object> listData = new ArrayList<Object>();
            objectLists.put(instanceId, listData);
        }

        // Determine column names
        String entityIdName = owningClass.getContainedIdName();
        String dataIdName = "";

        if (referenceType == null)
        {
            dataIdName = getDataName();
        }
        else if (isContained())
        {
            dataIdName = owningClass.getContainedIdName(this);
        }
        else
        {
            dataIdName = getReferenceIdName();
        }

        // Load each row of list data, one row at a time
        // Add the data from each row to the proper instances' list
        for (DataRow row : subTable.getRows())
        {
            DataField entityIdField = row.get(entityIdName);
            Object entityId = entityIdField.getValue();
            List<Object> list = objectLists.get(entityId);
            if (list != null)
            {
                if (referenceType == null)
                {
                    DataField dataField = row.get(dataIdName);
                    Object data = dataField.getValue();
                    list.add(data);
                }
                else if (isContained())
                {
                    Object newInstance = null;
                    try
                    {
                        newInstance = referenceType.createInstance(row);
                    }
                    catch (InvalidDataException e)
                    {
                    }
                    if (newInstance != null)
                    {
                        list.add(newInstance);
                    }
                }
                else
                {
                    DataField dataIdField = row.get(dataIdName);
                    Object dataId = dataIdField.getValue();
                    list.add(dataId);
                }
            }
        }

        // Assign lists to instance fields, or defer until later
        for (Object objectId : objectLists.keySet())
        {
            List<Object> listData = objectLists.get(objectId);
            Object instance = objectIdMap.get(objectId);

            if (referenceType == null || isContained())
            {
                set(instance, listData);
            }
            else
            {
                DeferredReferenceList list = deferredInstanceMap.get(instance);
                if (list == null)
                {
                    list = new DeferredReferenceList(this);
                    deferredInstanceMap.put(instance, list);
                }
                list.idList = listData;

                // Make sure this list gets touched next deferred load
                if (!deferredLists.contains(this))
                {
                    deferredLists.add(this);
                }
            }
        }
    }

    protected void populate(DataRow dataRow, Object instance, Object data)
    {
        populate(dataRow, instance, data, null);
    }

    protected void populate(DataRow dataRow, Object instance, Object data, PersistedField container)
    {
        PersistedField idField = owningClass.getIdField();

        // Add id row first, this binds to the owning class
        Object id = null;
        if (instance != null)
        {
            id = owningClass.getIdData(instance);
        }
        String idName = owningClass.getContainedIdName();
        DataField idData = new DataField(idName, idField.getDataType(), id);
        idData.setIdField(true);
        dataRow.add(idData);

        // Add data rows
        if (referenceType == null)
        {
            DataField valueData = new DataField(getDataName(), listDataType);
            valueData.setIdField(true);
            if (data != null)
            {
                valueData.setValue(data);
            }
            dataRow.add(valueData);
        }
        else if (isContained())
        {
            referenceType.populate(dataRow, data);
        }
        else
        {
            PersistedField referenceIdField = referenceType.getIdField();
            DataField referenceIdData = new DataField(getReferenceIdName(), referenceIdField.getDataType());
            if (data != null)
            {
                Object referenceId = referenceIdField.get(data);
                referenceIdData.setValue(referenceId);
            }
            referenceIdData.setIdField(true);
            dataRow.add(referenceIdData);
        }
    }

    @Override
    public void populateHeader(DataTable dataTable, PersistedField container)
    {
        dataTable.createHeader();
        DataRow headerRow = dataTable.getHeader();
        populate(headerRow, null, null, container);
    }

    public void save(DataTable table, Object instance)
    {
        if (instance == null)
        {
            return;
        }

        @SuppressWarnings("unchecked")
        List<? extends Object> list = (List<? extends Object>) get(instance);
        if (list == null)
        {
            return;
        }

        for (Object data : list)
        {
            DataRow row = new DataRow(table);
            populate(row, instance, data);
            table.addRow(row);
        }
    }
}
