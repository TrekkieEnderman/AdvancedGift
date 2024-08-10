/*
 * Copyright (c) 2024 TrekkieEnderman
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift;

import org.bukkit.Bukkit;

public class ServerVersion {

    // The common version format is major.minor.revision
    // I doubt I'll ever need to check the major version or the revision, but I'm including them anyway, just in case.
    // 1.20.5 edit: Welp, thanks Mojang.
    private final int MAJOR;
    private final int MINOR;
    private final int REVISION;
    private final String NMS;
    private static ServerVersion instance;

    private ServerVersion() {
        // Grab the server version and try to parse it as integers.
        String[] array = Bukkit.getServer().getBukkitVersion().split("-")[0].split("\\.");
        MAJOR = Integer.parseInt(array[0]);
        MINOR = Integer.parseInt(array[1]);
        REVISION = array.length > 2 ? Integer.parseInt(array[2]) : 0; // Assign the third number from the string if it exists, else assign 0

        // Get the NMS version if it exists, otherwise mark it as "unknown"
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String temp = packageName.replace("org.bukkit.craftbukkit", "").replace(".", "");
        NMS = !temp.isEmpty() ? temp : "unknown";
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
