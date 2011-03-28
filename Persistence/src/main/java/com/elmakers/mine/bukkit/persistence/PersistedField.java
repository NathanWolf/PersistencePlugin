package com.elmakers.mine.bukkit.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import com.elmakers.com.bukkit.data.exception.InvalidDataException;
import com.elmakers.mine.bukkit.data.DataField;
import com.elmakers.mine.bukkit.data.DataRow;
import com.elmakers.mine.bukkit.data.DataTable;
import com.elmakers.mine.bukkit.data.DataType;
import com.elmakers.mine.bukkit.persisted.FieldInfo;
import com.elmakers.mine.bukkit.persistence.exception.InvalidPersistedClassException;

/**
 * Represents a persisted field.
 * 
 * This can point to an actual field, or to a getter/setter pair.
 * 
 * @author NathanWolf
 * 
 */
public class PersistedField
{
    protected static Logger log = Persistence.getLogger();

    protected static String convertFieldName(String fieldName)
    {
        if (fieldName.length() > 0)
        {
            String fieldEnd = "";
            if (fieldName.length() > 1)
            {
                fieldEnd = fieldName.substring(1);
            }
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldEnd;
        }
        return fieldName;
    }

    protected static String dePluralize(String plural)
    {
        // Special cases- kinda hacky, but makes for clean schemas.
        if (plural.equals("children"))
        {
            return "child";
        }
        if (plural.equals("Children"))
        {
            return "Child";
        }

        if (plural.length() > 1 && plural.charAt(plural.length() - 1) == 's')
        {
            plural = plural.substring(0, plural.length() - 1);
        }

        return plural;
    }

    protected static Field findField(String fieldName, Class<? extends Object> c)
    {
        Field field = null;

        try
        {
            c.getField(fieldName);
        }
        catch (SecurityException e)
        {
            // log.warning("Persistence: Can't access field " + fieldName +
            // " of class " + c.getName());
        }
        catch (NoSuchFieldException e)
        {
            // log.warning("Persistence: Can't find field " + fieldName +
            // " of class " + c.getName());
        }

        return field;
    }

    protected static Method findGetter(String getterName, Class<?> c)
    {
        Method getter = null;
        try
        {
            getter = c.getMethod(getterName);
        }
        catch (NoSuchMethodException e)
        {
            getter = null;
        }

        return getter;
    }

    protected static Method findSetter(String setterName, Class<?> returnType, Class<? extends Object> c)
    {
        Method setter = null;
        try
        {
            setter = c.getMethod(setterName, returnType);
        }
        catch (NoSuchMethodException e)
        {
            setter = null;
        }
        return setter;
    }

    public static String getContainedName(String container, String contained)
    {
        String remainingContained = "";
        if (contained.length() > 1)
        {
            remainingContained = contained.substring(1);
        }

        // De plural-ize
        container = dePluralize(container);
        contained = container + contained.substring(0, 1).toUpperCase() + remainingContained;

        // Also de-pluralize results
        if (contained.length() > 1 && contained.charAt(contained.length() - 1) == 's')
        {
            contained = contained.substring(0, contained.length() - 1);
        }
        return dePluralize(contained);
    }

    protected static String getFieldFromMethod(Method method)
    {
        String methodName = method.getName();
        String fieldName = "";
        if (methodName.substring(0, 2).equals("is"))
        {
            fieldName = methodName.substring(2);
        }
        else
        {
            fieldName = methodName.substring(3);
        }
        if (fieldName.length() > 0)
        {
            String fieldEnd = "";
            if (fieldName.length() > 1)
            {
                fieldEnd = fieldName.substring(1);
            }
            fieldName = fieldName.substring(0, 1).toLowerCase() + fieldEnd;
        }

        return fieldName;
    }

    protected static String getGetterName(String fieldName)
    {
        return "get" + convertFieldName(fieldName);
    }

    protected static String getIsName(String fieldName)
    {
        return "is" + convertFieldName(fieldName);
    }

    protected static String getSetterName(String fieldName)
    {
        return "set" + convertFieldName(fieldName);
    }

    protected static boolean isGetter(Method method)
    {
        return method.getReturnType() != void.class && method.getParameterTypes().length == 0;
    }

    protected static boolean isSetter(Method method)
    {
        return method.getReturnType() == void.class && method.getParameterTypes().length == 1;
    }

    protected static PersistedField tryCreate(FieldInfo fieldInfo, Field field, PersistentClass owningClass)
    {
        DataType dataType = DataType.getTypeFromClass(field.getType());
        PersistedField pField = null;

        if (dataType == DataType.OBJECT)
        {
            pField = new PersistedObject(fieldInfo, field, owningClass);
        }
        else if (dataType == DataType.LIST)
        {
            pField = new PersistedList(fieldInfo, field, owningClass);
        }
        else if (dataType != DataType.NULL)
        {
            pField = new PersistedField(fieldInfo, field, owningClass);
        }

        return pField;
    }

    protected static PersistedField tryCreate(FieldInfo fieldInfo, Method getterOrSetter, PersistentClass owningClass)
    {
        Method setter = null;
        Method getter = null;
        String fieldName = fieldInfo.getName();
        if (fieldName == null || fieldName.length() == 0)
        {
            fieldName = getFieldFromMethod(getterOrSetter);
        }
        Class<? extends Object> persistClass = owningClass.getType();

        if (fieldName.length() == 0)
        {
            log.warning("Persistence: Field " + persistClass.getName() + "." + getterOrSetter.getName() + " has an invalid name");
            return null;
        }

        if (isSetter(getterOrSetter))
        {
            setter = getterOrSetter;
            String getterName = fieldInfo.getGetter();
            if (getterName == null || getterName.length() == 0)
            {
                getterName = getGetterName(fieldName);
            }
            getter = findGetter(getterName, persistClass);

            if (getter == null)
            {
                getterName = getIsName(fieldName);
                getter = findGetter(getterName, persistClass);
            }
        }
        else if (isGetter(getterOrSetter))
        {
            getter = getterOrSetter;
            String setterName = fieldInfo.getSetter();
            if (setterName == null || setterName.length() == 0)
            {
                setterName = getSetterName(getFieldFromMethod(getter));
            }
            setter = findSetter(setterName, getter.getReturnType(), persistClass);
        }

        if (getter == null)
        {
            log.warning("Persistence: Field " + persistClass.getName() + "." + getterOrSetter.getName() + " has no getter");
            return null;
        }

        if (setter == null && !fieldInfo.isReadOnly())
        {
            log.warning("Persistence: Field " + persistClass.getName() + "." + getterOrSetter.getName() + " has no setter and is not read only");
            return null;
        }

        PersistedField pField = null;
        DataType dataType = DataType.getTypeFromClass(getter.getReturnType());

        if (dataType == DataType.OBJECT)
        {
            pField = new PersistedObject(fieldInfo, getter, setter, owningClass);
        }
        else if (dataType == DataType.LIST)
        {
            pField = new PersistedList(fieldInfo, getter, setter, owningClass);
        }
        else if (dataType != DataType.NULL)
        {
            pField = new PersistedField(fieldInfo, getter, setter, owningClass);
        }

        return pField;
    }

    protected PersistedField        container = null;

    protected Field                 field     = null;

    protected FieldInfo             fieldInfo = null;

    protected Method                getter    = null;

    protected String                name      = null;

    protected final PersistentClass owningClass;

    protected Method                setter    = null;

    protected PersistedField(FieldInfo fieldInfo, Field field, PersistentClass owningClass)
    {
        this.name = fieldInfo.getName();
        if (name == null || name.length() == 0)
        {
            name = getFieldFromMethod(getter);
        }
        this.field = field;
        this.getter = null;
        this.setter = null;
        this.fieldInfo = fieldInfo;
        this.owningClass = owningClass;
    }

    protected PersistedField(FieldInfo fieldInfo, Method getter, Method setter, PersistentClass owningClass)
    {
        this.name = fieldInfo.getName();
        if (name == null || name.length() == 0)
        {
            name = getFieldFromMethod(getter);
        }
        this.getter = getter;
        this.setter = setter;
        this.field = null;
        this.fieldInfo = fieldInfo;
        this.owningClass = owningClass;
    }

    public PersistedField(PersistedField copy)
    {
        this.setter = copy.setter;
        this.getter = copy.getter;
        this.field = copy.field;
        this.name = copy.name;
        this.fieldInfo = copy.fieldInfo;
        this.owningClass = copy.owningClass;
    }

    public void bind() throws InvalidPersistedClassException
    {

    }

    @Override
    public PersistedField clone()
    {
        PersistedField field = new PersistedField(this);
        return field;
    }

    public Object get(Object o)
    {
        if (o == null)
        {
            return null;
        }

        Object result = null;
        if (getter != null)
        {
            try
            {
                result = getter.invoke(o);
            }
            catch (InvocationTargetException e)
            {
                result = null;
            }
            catch (IllegalAccessException e)
            {
                result = null;
            }
        }

        if (result == null && field != null)
        {
            try
            {
                result = field.get(o);
            }
            catch (IllegalAccessException e)
            {
                result = null;
            }
        }

        return result;
    }

    public PersistedField getConcreteField()
    {
        PersistedField concrete = this;
        PersistentClass reference = this.getReferenceType();
        if (reference != null)
        {
            concrete = reference.getConcreteIdField();
        }

        return concrete;
    }

    public String getDataName()
    {
        if (container != null && !(container instanceof PersistedList))
        {
            return getContainedName(container.getDataName(), name);
        }

        if (fieldInfo.getName().length() > 0)
        {
            return fieldInfo.getName();
        }

        return name;
    }

    public DataType getDataType()
    {
        Class<?> fieldType = getType();
        return DataType.getTypeFromClass(fieldType);
    }

    public FieldInfo getFieldInfo()
    {
        return fieldInfo;
    }

    public String getName()
    {
        return name;
    }

    public PersistentClass getReferenceType()
    {
        return null;
    }

    public Class<?> getType()
    {
        if (getter != null)
        {
            return getter.getReturnType();
        }
        if (field != null)
        {
            return field.getType();
        }
        return null;
    }

    /**
     * Check to see if this object is inside a container (it should be, if it's
     * a contained object - but _may_ be even if not contained!)
     * 
     * @return true if this is a contained field, due to containment by a parent
     */
    public boolean hasContainer()
    {
        return container != null;
    }

    public boolean isAutogenerated()
    {
        return fieldInfo.isAutogenerated();
    }

    /**
     * This will check to see if either entity info (annotations) specify this
     * is a contained field
     * 
     * @return true if this is a contained field
     */
    public boolean isContained()
    {
        return fieldInfo.isContained();
    }

    public boolean isIdField()
    {
        return fieldInfo.isIdField();
    }

    public boolean isReadOnly()
    {
        return fieldInfo.isReadOnly();
    }

    public void load(DataRow row, Object o) throws InvalidDataException
    {
        DataField dataField = row.get(getDataName());

        // Silently drop missing data...
        // TODO: Log print here?
        if (dataField != null)
        {
            set(o, dataField.getValue(getType()));
        }
    }

    public void populateHeader(DataTable dataTable)
    {
        populateHeader(dataTable, null);
    }

    public void populateHeader(DataTable dataTable, PersistedField container)
    {
        DataRow headerRow = dataTable.getHeader();
        DataField field = new DataField(getDataName(), getDataType());
        field.setIdField(isIdField());
        field.setAutogenerated(isAutogenerated());
        headerRow.add(field);
    }

    public void save(DataRow row, Object o) throws InvalidDataException
    {
        Object data = null;
        if (o != null)
        {
            data = get(o);
        }
        DataField field = new DataField(getDataName(), getDataType(), data);
        field.setIdField(isIdField());
        field.setAutogenerated(isAutogenerated());
        row.add(field);
    }

    public <T> boolean set(Object o, T value) throws InvalidDataException
    {
        if (setter == null)
        {
            if (isReadOnly())
            {
                log.warning("Persistence: attempt to set() on a field " + getName());
            }
            return false;
        }

        if (value == null && DataType.isPrimitive(getType()))
        {
            throw new InvalidDataException("Attempt to set null to primitive type for field " + getName());
        }

        if (setter != null)
        {
            try
            {
                setter.invoke(o, value);
            }
            catch (Throwable e)
            {
                throw new InvalidDataException(e);
            }
        }

        if (field != null)
        {
            try
            {
                field.set(o, value);
            }
            catch (Throwable e)
            {
                throw new InvalidDataException(e);
            }
        }
        return true;
    }

    public void setContainer(PersistedField container)
    {
        this.container = container;
    }
}
