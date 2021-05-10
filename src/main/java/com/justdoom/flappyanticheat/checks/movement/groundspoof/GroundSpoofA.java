package com.justdoom.flappyanticheat.checks.movement.groundspoof;

import com.justdoom.flappyanticheat.FlappyAnticheat;
import com.justdoom.flappyanticheat.checks.Check;
import com.justdoom.flappyanticheat.checks.CheckData;
import com.justdoom.flappyanticheat.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CheckData(name = "GroundSpoof", type = "A", experimental = false)
public class GroundSpoofA extends Check implements Listener {

    private final double groundY = 0.015625;

    //private int buffer = 0;
    private boolean lastOnGround, lastLastOnGround;

    public GroundSpoofA(){
        super("GroundSpoof", "A", true);
    }

    @EventHandler
    protected void check(PlayerMoveEvent e){
        Player player = e.getPlayer();
        PlayerData data = FlappyAnticheat.getInstance().dataManager.getData(player.getUniqueId());

        boolean client = player.isOnGround(), server = e.getTo().getY() % groundY < 0.0001;

        boolean onGround = isNearGround(e.getTo());

        boolean lastOnGround = this.lastOnGround;
        this.lastOnGround = onGround;

        boolean lastLastOnGround = this.lastLastOnGround;
        this.lastLastOnGround = lastOnGround;

        if (client && !server) {
            if(!onGround && !lastOnGround && !lastLastOnGround) {

                boolean boat = false;
                boolean shulker = false;

                List<Entity> nearby = player.getNearbyEntities(1.5, 10, 1.5);

                for (Entity entity : nearby) {
                    if (entity.getType() == EntityType.BOAT && player.getLocation().getY() > entity.getLocation().getY()) {
                        boat = true;
                        break;
                    }

                    if (entity.getType() == EntityType.SHULKER && player.getLocation().getY() > entity.getBoundingBox().getMinY()) {
                        shulker = true;
                        break;
                    }
                }

                for (Block block : getNearbyBlocks(new Location(player.getWorld(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()), 2)) {

                    if (Tag.SHULKER_BOXES.isTagged(block.getType())) {
                        shulker = true;
                        break;
                    }
                }

                if (!boat && !shulker) {
                    fail("mod=" + e.getTo().getY() % groundY, player);
                }
            }
        }// else if(buffer > 0) buffer--;
    }

    public boolean isNearGround(Location location){
        double expand = 0.3;
        for(double x = -expand; x <= expand; x += expand){
            for(double z = -expand; z <= expand; z += expand){
                if(location.clone().add(x, -0.5001, z).getBlock().getType() != Material.AIR){
                    return true;
                }
            }
        }
        return false;
    }

    public Set<Block> getNearbyBlocks(Location location, int radius) {
        Set<Block> blocks = new HashSet<>();

        for(int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
            for(int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
                for(int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                    blocks.add(location.getWorld().getBlockAt(x, y, z));
                }
            }
        }

        return blocks;
    }
}