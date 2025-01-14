package com.justdoom.flappyanticheat.checks.movement.groundspoof;

import com.justdoom.flappyanticheat.FlappyAnticheat;
import com.justdoom.flappyanticheat.checks.Check;
import com.justdoom.flappyanticheat.data.PlayerData;
import com.justdoom.flappyanticheat.utils.PlayerUtil;
import com.justdoom.flappyanticheat.utils.ServerUtil;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class GroundSpoofA extends Check {

    private int buffer = 0;

    public GroundSpoofA() {
        super("GroundSpoof", "A", false);
    }

    private Map<UUID, Boolean> lastInAir = new HashMap<>();
    private Map<UUID, Boolean> lastOnGround = new HashMap<>();

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent e) {
        Player player = e.getPlayer();

        if (e.getPacketId() == PacketType.Play.Client.POSITION || e.getPacketId() == PacketType.Play.Client.POSITION_LOOK) {

            WrappedPacketInFlying packet = new WrappedPacketInFlying(e.getNMSPacket());

            if(ServerUtil.lowTPS(("checks." + check + "." + checkType).toLowerCase()) || player.getLocation().getY() < 1 || player.isDead()){
                return;
            }

            if (player.isInsideVehicle()){
                return;
            }

            PlayerData data = FlappyAnticheat.getInstance().dataManager.getData(player.getUniqueId());

            double groundY = 0.015625;
            boolean client = packet.isOnGround(), server = packet.getY() % groundY < 0.0001;

            if (client && !server && !PlayerUtil.isOnClimbable(player)) {
                if (++buffer > 1) {

                    boolean boat = false;
                    boolean shulker = false;
                    boolean pistonHead = false;

                    AtomicReference<List<Entity>> nearby = new AtomicReference<>();
                    sync(() -> nearby.set(data.entities));

                    for (Entity entity : nearby.get()) {
                        if (entity.getType() == EntityType.BOAT && player.getLocation().getY() > entity.getLocation().getY()) {
                            boat = true;
                            break;
                        }

                        if (entity.getType() == EntityType.SHULKER && player.getLocation().getY() > entity.getBoundingBox().getMinY()) {
                            shulker = true;
                            break;
                        }
                    }

                    for (Block block : PlayerUtil.getNearbyBlocks(new Location(player.getWorld(), packet.getX(), packet.getY(), packet.getZ()), 2)) {

                        if (Tag.SHULKER_BOXES.isTagged(block.getType())) {
                            shulker = true;
                            break;
                        }

                        if (block.getType() == Material.PISTON_HEAD) {
                            pistonHead = true;
                            break;
                        }
                    }

                    if (!boat && !shulker && !pistonHead) {
                        String suspectedHack;
                        if(packet.getY() % groundY == 0.0){
                            suspectedHack = "Criticals/Anti Hunger";
                        } else {
                            suspectedHack = "NoFall";
                        }
                        fail("mod=" + packet.getY() % groundY + " &7Client: &2" + client + " &7Server: &2" + server + " &7Suspected Hack: &2" + suspectedHack, player);
                    }
                }
            } else if (buffer > 0) buffer-=0.5;

            boolean inAir = true;

            boolean lastInAir = this.lastInAir.getOrDefault(player.getUniqueId(), false);

            for (Block block : PlayerUtil.getNearbyBlocksConfigurable(new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY() -1, player.getLocation().getZ()), 1, 0, 1)) {
                if (block.getType() != Material.AIR) {
                    inAir = false;
                    break;
                }
            }

            boolean lastOnGround = this.lastOnGround.getOrDefault(player.getUniqueId(), true);

            //check if they have packet on ground, are in the air, and were last in the air.
            if (packet.isOnGround() && lastOnGround && inAir && lastInAir) {
                String suspectedHack = "NoFall";

                fail("mod=" + packet.getY() % groundY + " &7Client: &2" + client + " &7Server: &2" + server + " &7Suspected Hack: &2" + suspectedHack, player);

            }

            this.lastOnGround.put(player.getUniqueId(), packet.isOnGround());
            this.lastInAir.put(player.getUniqueId(), inAir);
        }
    }
}