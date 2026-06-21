package myau.module.modules.render;

import myau.module.Module;
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
    public final myau.property.properties.DragProperty drag = new myau.property.properties.DragProperty("Position", new myau.util.vector.Vector2d(0, 0));
    public float defaultX = 0;
    public float defaultY = 0;

    public Scoreboard() {
        super("Scoreboard", true, false);
    }

    public void updateBounds(ScaledResolution scaledRes) {
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

        this.defaultX = baseX;
        this.defaultY = baseY;

        // Ensure drag property is initialized reasonably if it's 0,0
        if (this.drag.position.x == 0 && this.drag.position.y == 0 && this.drag.targetPosition.x == 0) {
            this.drag.position.x = baseX;
            this.drag.position.y = baseY;
            this.drag.targetPosition.x = baseX;
            this.drag.targetPosition.y = baseY;
        }

        this.drag.scale.x = width;
        this.drag.scale.y = height;
    }
}
