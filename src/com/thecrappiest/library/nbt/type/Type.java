package com.thecrappiest.library.nbt.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thecrappiest.library.nbt.util.Utility;
import com.thecrappiest.library.nbt.version.ServerVersion;

public enum Type {

    LIST,
    STRING,
    DOUBLE,
    SHORT,
    FLOAT,
    LONG,
    INTEGER,
    INTEGER_ARRAY,
    BYTE,
    BYTE_ARRAY;
    
    private static Map<Type, Class<?>> cache_compound_class = new HashMap<>();
    private static Map<Type, Field> cache_field = new HashMap<>();
    private static Map<Type, Constructor<?>> cache_constructor = new HashMap<>();
    
    public Class<?> compoundClass() {
        if(cache_compound_class.containsKey(this))
            return cache_compound_class.get(this);
        
        boolean legacy = ServerVersion.currentValue() < 10;
        String class_path = (legacy ? "net.minecraft.server."+ServerVersion.current().name()+"." : "net.minecraft.nbt.") + "NBTTag";
        
        Class<?> class_compound = switch(this) {
        case LIST -> get(class_path + "List");
        case BYTE -> get(class_path + "Byte");
        case BYTE_ARRAY -> get(class_path + "ByteArray");
        case DOUBLE -> get(class_path + "Double");
        case FLOAT -> get(class_path + "Float");
        case INTEGER -> get(class_path + "Int");
        case INTEGER_ARRAY -> get(class_path + "IntArray");
        case LONG -> get(class_path + "Long");
        case SHORT -> get(class_path + "Short");
        case STRING -> get(class_path + "String");
        };
        
        cache_compound_class.put(this, class_compound);
        return class_compound;
    }
    
    private Class<?> get(String path) {
        return Utility.instance().get(path);
    }
    
    public Constructor<?> compoundConstructor() {
        if(cache_constructor.containsKey(this))
            return cache_constructor.get(this);
        
        Class<?> primitive = switch(this) {
        case LIST -> List.class;
        case BYTE -> byte.class;
        case BYTE_ARRAY -> byte[].class;
        case DOUBLE -> double.class;
        case FLOAT -> float.class;
        case INTEGER -> int.class;
        case INTEGER_ARRAY -> int[].class;
        case LONG -> long.class;
        case SHORT -> short.class;
        case STRING -> String.class;
        };
        
        try {
            Constructor<?> constructor = this == LIST ? compoundClass().getConstructor() : compoundClass().getConstructor(primitive);
            cache_constructor.put(this, constructor);
            return constructor;
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Object constructCompound(Object object) {
        Constructor<?> constructor = compoundConstructor();
        
        try {
            return this == LIST ? Utility.instance().newCompoundList((List<?>) object) : constructor.newInstance(object);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Field typeField() {
        if(cache_field.containsKey(this))
            return cache_field.get(this);
        
        boolean legacy = ServerVersion.currentValue() < 10;
        
        String name = switch(this) {
        case LIST -> legacy ? "list" : "c";
        case BYTE -> legacy ? "data" : "x";
        case BYTE_ARRAY -> legacy ? "data" : "c";
        case DOUBLE -> legacy ? "data" : "w";
        case FLOAT -> legacy ? "data" : "w";
        case INTEGER -> legacy ? "data" : "c";
        case INTEGER_ARRAY -> legacy ? "data" : "c";
        case LONG -> legacy ? "data" : "c";
        case SHORT -> legacy ? "data" : "c";
        case STRING -> legacy ? "data" : "A";
        };
        
        try {
            Field field = compoundClass().getField(name);
            cache_field.put(this, field);
            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Object fromCompound(Object compound) {
        Field field = typeField();
        
        try {
            return field.get(compound);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }
}