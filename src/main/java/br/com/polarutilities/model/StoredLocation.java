package br.com.polarutilities.model;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
    public static StoredLocation from(Location location) {
        World locationWorld = location.getWorld();
        String worldName = locationWorld == null ? "world" : locationWorld.getName();
        return new StoredLocation(
            worldName,
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    public static Optional<StoredLocation> fromSection(ConfigurationSection section) {
        if (section == null || !section.isString("world")) {
            return Optional.empty();
        }
        return Optional.of(new StoredLocation(
            section.getString("world", "world"),
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        ));
    }

    public void saveTo(ConfigurationSection section) {
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
    }

    public Optional<Location> toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(bukkitWorld, x, y, z, yaw, pitch));
    }

    public String shortText() {
        return world + " " + Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z);
    }
}
