package com.justdoom.flappyanticheat.checks.movement.fly;

import com.justdoom.flappyanticheat.checks.Check;
import com.justdoom.flappyanticheat.utils.PlayerUtil;
import com.justdoom.flappyanticheat.utils.ServerUtil;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyA extends Check {

    private Map<UUID, Double> airTicks = new HashMap<>();
    private Map<UUID, Double> buffer = new HashMap<>();
    private Map<UUID, Double> lastDeltaY = new HashMap<>();

    private Map<UUID, Boolean> inAir = new HashMap<>();

    public FlyA(){
        super("Fly", "A", false);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        double airTicks = this.airTicks.getOrDefault(uuid, 0.0);
        boolean inAir = this.inAir.getOrDefault(uuid, false);

        //measuring how many ticks the player has an air block below them for
        airTicks = inAir ? airTicks + 1 : 0;

        this.airTicks.put(uuid, airTicks);

        if (event.getPacketId() == PacketType.Play.Client.POSITION || event.getPacketId() == PacketType.Play.Client.POSITION_LOOK) {

            WrappedPacketInFlying packet = new WrappedPacketInFlying(event.getNMSPacket());

            //dont run the check if they have /fly on or are creative flying
            if (player.isFlying() || player.isGliding()) return;

            if(ServerUtil.lowTPS(("checks." + check + "." + checkType).toLowerCase()))
                return;

            //check if the blocks below the player are air blocks. not entirely accurate.
            for (Block block : PlayerUtil.getNearbyBlocksHorizontally(new Location(player.getWorld(),
                    player.getLocation().getX(), player.getLocation().getY() - 1, player.getLocation().getZ()), 1)) {
                if (block.getType() != Material.AIR) {
                    this.inAir.put(uuid, false);
                    break;
                } else {
                    this.inAir.put(uuid, true);
                }
            }

            final double deltaY = packet.getPosition().getY() - player.getLocation().getY();

            final double lastDeltaY = this.lastDeltaY.getOrDefault(uuid, 0.0);
            this.lastDeltaY.put(uuid, deltaY);

            //i dont believe you need all of the extra? also, math.abs is causing falses :///
            final double prediction = ((lastDeltaY - 0.08) * 0.98F);
            final double difference = Math.abs(deltaY - prediction);

            final boolean invalid = difference > 0.00001D
                    && airTicks > 8
                    //was able to replace this all due to my new air block check LMFAO.
                    //note: this is not accurate if youre inside an enclosed space/near blocks. simply a quick fix i made
                    ;

            double buffer = this.buffer.getOrDefault(uuid, 0.0);

            if (invalid) {
                buffer += buffer < 50 ? 10 : 0;
                if (buffer > 20) {
                    fail("diff=%.4f, buffer=%.2f, at=%o", player);
                }
            } else {
                buffer = Math.max(buffer - 0.75, 0);
            }

            this.buffer.put(uuid, buffer);
        }
    }
}