package com.thecrappiest.library.nbt.version;

import org.bukkit.Bukkit;

public enum ServerVersion {

    V1_8_R3,//0
    V1_9_R2,//1
    V1_10_R1,//2
    V1_11_R1,//3
    V1_12_R1,//4
    V1_13_R2,//5
    V1_14_R1,//6
    V1_15_R1,//7
    V1_16_R3,//8
    V1_17_R1,//9
    V1_18_R2,//10
    V1_19_R3,//11
    V1_20_R3;//12
    
    private static ServerVersion cachedVersion;
    public static ServerVersion current() {
        if(cachedVersion != null) 
            return cachedVersion;
        
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        cachedVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", "").toUpperCase());
        
        return cachedVersion;
    }
    
    public static int currentValue() {
        return current().ordinal();
    }
    
}