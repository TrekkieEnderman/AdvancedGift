package io.github.TrekkieEnderman.advancedgift;

import org.bukkit.Bukkit;

public class ServerVersion {

    //The common version format is major.minor.revision
    //I doubt I'll ever need to check the major version or the revision, but I'm including them anyway, just in case.
    private final int MAJOR;
    private final int MINOR;
    private final int REVISION;
    private final String NMS;
    private static ServerVersion instance;

    private ServerVersion() {
        //Grabs the server version and NMS version on initialization, and tries to parse the former as integers.
        String[] array = Bukkit.getServer().getBukkitVersion().split("-")[0].split("\\.");
        MAJOR = Integer.parseInt(array[0]);
        MINOR = Integer.parseInt(array[1]);
        REVISION = array.length > 2 ? Integer.parseInt(array[2]) : 0; //Assign the third number from the string if it exists, else assign 0

        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        NMS = packageName.substring(packageName.lastIndexOf('.')+1);
    }

    static void init() {
        if (instance == null) instance = new ServerVersion();
    }

    public static int getMajorVersion() {
        return instance.MAJOR;
    }

    public static int getMinorVersion() {
        return instance.MINOR;
    }

    public static int getRevisionVersion() {
        return instance.REVISION;
    }

    public static String getNMSVersion() {
        return instance.NMS;
    }
}
