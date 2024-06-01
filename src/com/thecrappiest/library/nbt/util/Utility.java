package com.thecrappiest.library.nbt.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.thecrappiest.library.nbt.type.Type;
import com.thecrappiest.library.nbt.version.ServerVersion;

public class Utility {

    private static Utility instance = new Utility();
    private ServerVersion currentVersion = ServerVersion.current();
    private String ver = currentVersion.name();
    private int version = currentVersion.ordinal();
    private boolean legacy = version < 9;
    
    private String obc = "org.bukkit.craftbukkit."+ver;
    private String nms = "net.minecraft.server."+ver;
    private String nbt = (legacy ? "net.minecraft.server."+ver+"." : "net.minecraft.nbt.") + "NBT";
    
    private Class<?> class_craft_itemstack = get(obc+".inventory.CraftItemStack");
    private Class<?> class_minecraft_itemstack = get((legacy ? nms+"." : "net.minecraft.world.item.") + "ItemStack");
    private Class<?> class_tag = get(nbt + "TagCompound");
    private Class<?> class_compound_base = get(nbt + "Base");
    private Class<?> class_compound_list = get(nbt + "TagList");
    private Class<?> class_datacomponent_map = version > 11 ? get("net.minecraft.core.component.DataComponentMap") : null;
    private Class<?> class_datacomponent_type = version > 11 ? get("net.minecraft.core.component.DataComponentType") : null;
    private Class<?> class_datacomponents = version > 11 ? get("net.minecraft.core.component.DataComponents") : null;
    private Class<?> class_component_customdata = version > 11 ? get("net.minecraft.world.item.component.CustomData") : null;
    
    private Field field_itemstack_handle = null;
    {
        try {
            field_itemstack_handle = class_craft_itemstack.getField("handle");
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
    }
    
    private Method method_copy_minecraft, method_copy_craft, method_copy_bukkit = null; 
    private Method method_minecraft_tag_get, method_minecraft_tag_set = null;
    private Method method_componentmap_get, method_componentmap_set, method_componentmap_type, method_build_customdata, method_customdata_tag = null;
    private Method method_tag_get, method_tag_set, method_tag_keys, method_tag_key_has = null;
    private Method method_compound_list_add = null;
    private Method method_compound_list_size = null;
    
    private Constructor<?> constructor_tag = null;
    {
        try {
            method_copy_minecraft = class_craft_itemstack.getMethod("asNMSCopy");
            method_copy_craft = class_craft_itemstack.getMethod("asCraftCopy");
            method_copy_bukkit = class_craft_itemstack.getMethod("asBukkitCopy");
            
            if(version == 11 || version < 11) {
                method_minecraft_tag_set = class_minecraft_itemstack.getMethod(legacy ? "setTag" : "c", class_tag);
                method_minecraft_tag_get = class_minecraft_itemstack.getMethod(
                        switch(version) {
                        default -> "getTag";
                        case 10, 11, 12 -> "v";
                        });
            }else {
                method_componentmap_get = class_minecraft_itemstack.getDeclaredMethod("a");
                method_componentmap_set = class_minecraft_itemstack.getDeclaredMethod("b", class_datacomponent_type, Object.class);
                method_componentmap_type = class_datacomponent_map.getDeclaredMethod("a", class_datacomponent_type);
                method_build_customdata = class_component_customdata.getDeclaredMethod("a", class_tag);
                method_customdata_tag = class_component_customdata.getDeclaredMethod("c");
            }
            
            method_minecraft_tag_set = class_minecraft_itemstack.getMethod(legacy ? "setTag" : "c", class_tag);
            method_minecraft_tag_get = class_minecraft_itemstack.getMethod(
                    switch(version) {
                    default -> "getTag";
                    case 10, 11, 12 -> "v";
                    });
            
            method_tag_get = class_tag.getMethod(legacy ? "get" : "c", String.class);
            method_tag_set = class_tag.getMethod(legacy ? "set" : "a", String.class, class_compound_base);
            method_tag_key_has = class_tag.getMethod(legacy ? "hasKey" : "e", String.class);
            method_tag_keys = class_tag.getMethod(
                    switch(version) {
                    default -> "getKeys";
                    case 1, 2, 3, 4 -> "c";
                    case 10 -> "d";
                    case 11, 12 -> "e";
                    });
            
            method_compound_list_add = class_compound_list.getMethod("add", version < 12 ?
                    new Class<?>[] {class_compound_base} : new Class<?>[] {int.class, class_compound_base});
            method_compound_list_size = class_compound_list.getMethod("size");
            
            constructor_tag = class_tag.getConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }
    
    public static Utility instance() {
        return instance;
    }
    
    public Class<?> get(String class_name) {
        try {
            return Class.forName(class_name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Object newTag() {
        try {
            return constructor_tag.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Object createTag(ConfigurationSection section) {
        Object compound_tag = newTag();
        
        section.getKeys(false).forEach(tag_name -> {
            Object value = section.get(tag_name);
            setCompoundInTag(compound_tag, tag_name, section.isConfigurationSection(tag_name) ? (ConfigurationSection) value : value);
        });
        
        return compound_tag;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object getTag(Object minecraft_item) {
        try {
            if(version == 11 || version < 11)
                return method_minecraft_tag_get.invoke(minecraft_item);
            
            Object componentMap = method_componentmap_get.invoke(minecraft_item);
            Object typeData = method_componentmap_type.invoke(componentMap, Enum.valueOf((Class<Enum>) class_datacomponents, "b"));
            
            return typeData != null ? method_customdata_tag.invoke(typeData) : null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setTag(Object minecraft_item, Object tag) {
        try {
            if(version == 11 || version < 11) {
                method_minecraft_tag_set.invoke(minecraft_item, tag);
            }else {
                Object data = method_build_customdata.invoke(class_component_customdata, tag);
                method_componentmap_set.invoke(minecraft_item, Enum.valueOf((Class<Enum>) class_datacomponents, "b"), data);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    public Object getCompoundFromItem(Object minecraft_item, String name) {
        Object tag = getTag(minecraft_item);
        return tag == null ? null : getCompoundFromTag(tag, name);
    }
    
    public Object getCompoundFromTag(Object tag, String name) {
        try {
            return method_tag_get.invoke(tag, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void setCompoundInItem(Object minecraft_item, String name, Object value) {        
        Object tag = getTag(minecraft_item);
        if(tag != null)
            setCompoundInTag(tag, name, value);
    }
    
    public void setCompoundInTag(Object tag, String name, Object value) {
        Object compound = newCompound(value);
        if(compound == null)
            return;
        
        try {
            method_tag_set.invoke(tag, name, compound);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    public boolean doesItemContainKey(Object minecraft_item, String name) {
        Object tag = getTag(minecraft_item);
        return tag == null ? false : doesTagContainKey(tag, name);
    }
    
    public boolean doesTagContainKey(Object tag, String name) {
        try {
            return (boolean) method_tag_key_has.invoke(tag, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Set<String> getKeysFromItem(Object minecraft_item) {
        Object tag = getTag(minecraft_item);
        return tag == null ? null : getKeysFromTag(tag);
    }
    
    @SuppressWarnings("unchecked")
    public Set<String> getKeysFromTag(Object tag) {
        try {
            return (Set<String>) method_tag_keys.invoke(tag);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean isCompound(Object value) {
        Class<?> value_class = value.getClass();
        return value_class == class_tag || Stream.of(Type.values()).anyMatch(type -> value_class == type.compoundClass());
    }
    
    public Object newCompound(Object value) {
        if(isCompound(value))
            return value;
        
        Type type = switch(value) {
        case String s -> Type.STRING;
        case Boolean b -> Type.STRING;
        case Double d -> Type.DOUBLE;
        case Long l -> Type.LONG;
        case Short sh -> Type.SHORT;
        case Float f -> Type.FLOAT;
        case Integer i -> Type.INTEGER;
        case Integer[] ia -> Type.INTEGER_ARRAY;
        case Byte by -> Type.BYTE;
        case Byte[] bya -> Type.BYTE_ARRAY;
        default -> null;
        };
        
        if(type == null)
            return null;
        
        return type.constructCompound(value);
    }
    
    public Object newCompoundList(List<?> value) {
        try {
            Object compoundList = class_compound_list.getConstructor().newInstance();
            value.forEach(obj -> addCompoundToList(compoundList, obj));
            return compoundList;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void addCompoundToList(Object compound, Object value) {
        Object value_compound = newCompound(value);
        try {
            if(version < 12)
                method_compound_list_add.invoke(compound, value_compound);
            else
                method_compound_list_add.invoke(compound, getCompoundListSize(compound), value_compound);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    public int getCompoundListSize(Object compound) {
        try {
            return (int) method_compound_list_size.invoke(compound);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public Object asMinecraft(Object item) {
        try {
            if(item.getClass() == class_craft_itemstack)
                return field_itemstack_handle.get(item);
            else
                return method_copy_minecraft.invoke(class_craft_itemstack);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Object asCraft(ItemStack item) {
        try {
            return method_copy_craft.invoke(class_craft_itemstack);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public ItemStack asBukkit(Object minecraft_item) {
        try {
            return (ItemStack) method_copy_bukkit.invoke(minecraft_item);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    
}