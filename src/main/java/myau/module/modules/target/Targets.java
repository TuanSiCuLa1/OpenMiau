package myau.module.modules.target;

import myau.module.modules.misc.AntiBot;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class Targets extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty invisibles = new BooleanProperty("invisibles", false, this.players::getValue);
    public final BooleanProperty bosses = new BooleanProperty("bosses", false);
    public final BooleanProperty mobs = new BooleanProperty("mobs", false);
    public final BooleanProperty animals = new BooleanProperty("animals", false);
    public final BooleanProperty golems = new BooleanProperty("golems", false);
    public final BooleanProperty silverfish = new BooleanProperty("silverfish", false);
    public final BooleanProperty teams = new BooleanProperty("teams", true);

    public Targets() {
        super("Targets", true, true);
    }

    public boolean isValid(EntityLivingBase entityLivingBase) {
        if (entityLivingBase == null || mc.theWorld == null || mc.thePlayer == null) {
            return false;
        }
        if (!mc.theWorld.loadedEntityList.contains(entityLivingBase)) {
            return false;
        }
        if (entityLivingBase == mc.thePlayer || entityLivingBase == mc.thePlayer.ridingEntity) {
            return false;
        }
        if (entityLivingBase == mc.getRenderViewEntity() || entityLivingBase == mc.getRenderViewEntity().ridingEntity) {
            return false;
        }
        if (entityLivingBase.deathTime > 0) {
            return false;
        }
        if (entityLivingBase instanceof EntityOtherPlayerMP) {
            return this.isValidPlayer((EntityPlayer) entityLivingBase);
        }
        if (entityLivingBase instanceof EntityDragon || entityLivingBase instanceof EntityWither) {
            return this.bosses.getValue();
        }
        if (entityLivingBase instanceof EntityMob || entityLivingBase instanceof EntitySlime) {
            if (entityLivingBase instanceof EntitySilverfish) {
                return this.silverfish.getValue() && this.allowTeamColor(entityLivingBase);
            }
            return this.mobs.getValue();
        }
        if (entityLivingBase instanceof EntityAnimal
                || entityLivingBase instanceof EntityBat
                || entityLivingBase instanceof EntitySquid
                || entityLivingBase instanceof EntityVillager) {
            return this.animals.getValue();
        }
        if (entityLivingBase instanceof EntityIronGolem) {
            return this.golems.getValue() && this.allowTeamColor(entityLivingBase);
        }
        return false;
    }

    public boolean isValidPlayer(EntityPlayer player) {
        if (!this.players.getValue()) {
            return false;
        }
        boolean isInvisible = player.isInvisible();
        if (isInvisible && !this.invisibles.getValue()) {
            return false;
        }
        if (TeamUtil.isFriend(player)) {
            return false;
        }
        return this.allowSameTeam(player) && (isInvisible || !AntiBot.isBot(player));
    }

    public boolean allowTeamColor(EntityLivingBase entityLivingBase) {
        return this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase);
    }

    public boolean allowSameTeam(EntityPlayer player) {
        return this.teams.getValue() || !TeamUtil.isSameTeam(player);
    }
}
