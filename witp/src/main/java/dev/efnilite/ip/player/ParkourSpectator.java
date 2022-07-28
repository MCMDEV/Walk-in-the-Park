package dev.efnilite.ip.player;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.generator.base.ParkourGenerator;
import dev.efnilite.ip.leaderboard.Leaderboard;
import dev.efnilite.ip.player.data.PreviousData;
import dev.efnilite.ip.player.data.Score;
import dev.efnilite.ip.session.Session;
import dev.efnilite.ip.util.Util;
import dev.efnilite.ip.util.config.Option;
import dev.efnilite.vilib.util.Task;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for spectators of a Session.
 *
 * @author Efnilite
 */
public class ParkourSpectator extends ParkourUser {

    protected ParkourPlayer closest;
    protected BukkitTask closestChecker;
    protected final Session session;

    public ParkourSpectator(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, previousData);

        this.session = session;
        this.closest = session.getPlayers().get(0);

        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(closest.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvisible(true);
        player.setCollidable(false);

        sendTranslated("spectator");
        runClosestChecker();
    }

    public void updateScoreboard() {
        if (!Option.SCOREBOARD_ENABLED) {
            return;
        }

        // board can be null a few ticks after on player leave
        if (board == null) {
            return;
        }
        ParkourPlayer player = closest;
        ParkourGenerator generator = player.generator;

        Leaderboard leaderboard = player.getGenerator().getGamemode().getLeaderboard();

        String title = Util.translate(player.getPlayer(), Option.SCOREBOARD_TITLE);
        List<String> lines = new ArrayList<>();

        Score top = null, rank = null;
        if (leaderboard != null) {

            // only get score at rank if lines contains variables
            if (Util.listContains(lines, "topscore", "topplayer")) {
                top = leaderboard.getScoreAtRank(1);
            }

            rank = leaderboard.get(player.getUUID());
        }

        // set generic score if score is not found
        top = top == null ? new Score("?", "?", "?", 0) : top;
        rank = rank == null ? new Score("?", "?", "?", 0) : rank;


        // update lines
        for (String line : Option.SCOREBOARD_LINES) {
            line = Util.translate(player.getPlayer(), line); // add support for PAPI placeholders in scoreboard

            lines.add(line
                    .replace("%score%", Integer.toString(generator.getScore()))
                    .replace("%time%", generator.getTime())
                    .replace("%highscore%", Integer.toString(rank.score()))
                    .replace("%topscore%", Integer.toString(top.score()))
                    .replace("%topplayer%", top.name()).replace("%session%", getSession().getSessionId()));
        }

        board.updateTitle(title
                .replace("%score%", Integer.toString(generator.getScore()))
                .replace("%time%", generator.getTime())
                .replace("%highscore%", Integer.toString(rank.score()))
                .replace("%topscore%", Integer.toString(top.score()))
                .replace("%topplayer%", top.name()).replace("%session%", getSession().getSessionId()));
        board.updateLines(lines);
    }

    /**
     * Runs a checker which checks for the closest {@link ParkourPlayer}, and updates the scoreboard, etc. accordingly.
     */
    public void runClosestChecker() {
        closestChecker = Task.create(IP.getPlugin())
                .async()
                .execute(() -> {
                    if (session.getPlayers().size() < 2) { // only update if there is more than 1 player
                        return;
                    }

                    double leastDistance = 1000000;
                    ParkourPlayer closest = null;

                    for (ParkourPlayer pp : session.getPlayers()) {
                        double distance = pp.getLocation().distance(player.getLocation());
                        if (distance < leastDistance) {
                            closest = pp;
                            leastDistance = distance;
                        }
                    }

                    setClosest(closest == null ? session.getPlayers().get(0) : closest);

                    updateVisualTime(getClosest().selectedTime);
                })
                .repeat(10)
                .run();
    }

    /**
     * Stops the closest checker runnable.
     */
    public void stopClosestChecker() {
        closestChecker.cancel();
    }

    public void setClosest(ParkourPlayer closest) {
        this.closest = closest;
    }

    public ParkourPlayer getClosest() {
        return closest;
    }

    @Override
    public Session getSession() {
        return session;
    }
}