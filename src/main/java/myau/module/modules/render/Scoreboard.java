package myau.module.modules.render;

import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Scoreboard extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty offX = new IntProperty("offset-x", 0, -2000, 2000);
    public final IntProperty offY = new IntProperty("offset-y", 0, -2000, 2000);

    public Scoreboard() {
        super("Scoreboard", true, false);
    }

    public static class ScoreboardBounds {
        public float x, y, width, height;
        public ScoreboardBounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public ScoreboardBounds getBounds(ScaledResolution scaledRes) {
        net.minecraft.scoreboard.Scoreboard sb = null;
        ScoreObjective objective = null;
        if (mc.theWorld != null) {
            sb = mc.theWorld.getScoreboard();
            if (sb != null) {
                objective = sb.getObjectiveInDisplaySlot(1);
            }
        }

        int size;
        int maxWidth;
        if (objective != null && sb != null) {
            Collection<Score> collection = sb.getSortedScores(objective);
            List<Score> list = new ArrayList<>();
            for (Score score : collection) {
                if (score.getPlayerName() != null && !score.getPlayerName().startsWith("#")) {
                    list.add(score);
                }
            }
            if (list.size() > 15) {
                list = list.subList(list.size() - 15, list.size());
            }
            size = list.size();
            maxWidth = mc.fontRendererObj.getStringWidth(objective.getDisplayName());
            for (Score score : list) {
                ScorePlayerTeam team = sb.getPlayersTeam(score.getPlayerName());
                String name = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()) + ": " + score.getScorePoints();
                maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(name));
            }
        } else {
            size = 5;
            maxWidth = 80;
        }

        int width = maxWidth + 8;
        int height = size * mc.fontRendererObj.FONT_HEIGHT + 9;
        
        float baseX = scaledRes.getScaledWidth() - width - 2;
        float baseY = scaledRes.getScaledHeight() / 2 - height / 3;

        baseX += this.offX.getValue();
        baseY += this.offY.getValue();

        return new ScoreboardBounds(baseX, baseY, width, height);
    }
}
