package myau.mixin;

import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;

@SideOnly(Side.CLIENT)
@Mixin({S3EPacketTeams.class})
public interface IAccessorS3EPacketTeams {
    @Accessor("name")
    String getTeamName();

    @Accessor("players")
    Collection<String> getPlayers();
}
