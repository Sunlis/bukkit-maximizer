package ca.sunlis.maximizer;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Maximizer extends JavaPlugin {
	
	private static boolean enabled = false;
	private static int minCount = 0;
	private static int loginTimeout = 600;
	private static int kickTimeout = 600;
	
	private static int countDown = 0;
	private static boolean counting = false;
	private static boolean coolDown = false;
	
	private static int repeating;
	private static int repeatDelay = 10;
	
	public static Maximizer instance = new Maximizer();
	public static Server server = Maximizer.instance.getServer();
	
	@Override
	public void onEnable() {
		Maximizer.enabled = true;
		
//		System.out.println("Maximizer enabled");

		Maximizer.server = getServer();
		
		this.saveDefaultConfig();
		Maximizer.minCount = Math.max(2, Integer.parseInt(this.getConfig().getString("minimumcount")));
		Maximizer.loginTimeout = Math.max(10, Integer.parseInt(this.getConfig().getString("logintimeout")));
		Maximizer.kickTimeout = Math.max(0, Integer.parseInt(this.getConfig().getString("kicktimeout")));
		
//		System.out.println("Maximizer: min="+Maximizer.minCount+", login="+Maximizer.loginTimeout+", kick="+Maximizer.kickTimeout);
		
		Maximizer.countDown = (int) Math.ceil(loginTimeout/10);
		
		getServer().getPluginManager().registerEvents(new LoginListener(), this);
		
		startRepeat();
	}
	
	private void startRepeat() {
		Maximizer.repeating = Maximizer.server.getScheduler().scheduleAsyncRepeatingTask(
				Maximizer.instance, new Repeater(), 20L, 20L * Maximizer.repeatDelay);
	}
	
	private class Repeater implements Runnable {
		@Override
		public void run() {
			Maximizer.instance.countCheck();
		}
	}
	
	@Override
	public void onDisable() {
		Maximizer.enabled = false;
		Maximizer.server.getScheduler().cancelTask(Maximizer.repeating);
		
//		System.out.println("Maximizer disabled");
	}

	public void countCheck(){
//		System.out.println("Checking count... (" + getNumPlayers() + ")");
		if (Maximizer.coolDown) {
			if (Maximizer.countDown > 0) {
				Maximizer.countDown--;
				Maximizer.counting = false;
			} else {
				Maximizer.coolDown = false;
				Maximizer.countDown = Maximizer.loginTimeout/10;
			}
		} else {
			if (Maximizer.countDown > 0) {
				if (shouldKick()) {
					if (!Maximizer.counting || Maximizer.countDown % 3 == 0 || Maximizer.countDown == 1) {						
						Maximizer.server.broadcastMessage("There are too few players (minimum is " + Maximizer.minCount + ")");
						Maximizer.server.broadcastMessage((Maximizer.countDown * 10) + " seconds until kick");
					}
					Maximizer.counting = true;
					Maximizer.countDown--;
				} else {
					if (Maximizer.counting) {
						Maximizer.server.broadcastMessage("Kick cancelled. Have fun!");
					}
					Maximizer.counting = false;
					Maximizer.coolDown = false;
					Maximizer.countDown = Maximizer.loginTimeout/10;
				}
			} else {
				if (shouldKick()) {
					for (Player player : Maximizer.server.getOnlinePlayers()) {
						player.kickPlayer("There were too few players (minimum is " + Maximizer.minCount + 
								"). You can log back in after " + ((int)(Maximizer.kickTimeout/10) * 10) + " seconds.");
					}
					Maximizer.coolDown = true;
					Maximizer.counting = false;
					Maximizer.countDown = Maximizer.kickTimeout/10;					
				} else {
					if (Maximizer.counting) {
						Maximizer.server.broadcastMessage("Kick cancelled. Have fun!");
					}
					Maximizer.counting = false;
					Maximizer.coolDown = false;
					Maximizer.countDown = Maximizer.loginTimeout/10;
				}
			}
		}
	}
	
	private int getNumPlayers() {
		if (Maximizer.server == null) {
			return -1;
		}
		Player[] players = Maximizer.server.getOnlinePlayers();
		if (players == null) {
			return 0;
		}
		return players.length;
	}
	
	private boolean shouldKick() {
		return Maximizer.enabled && getNumPlayers() > 0 && getNumPlayers() < Maximizer.minCount;
	}

	/*
	 * Prevents people from joining while cooling down after a kick
	 */
	public class LoginListener implements Listener {
		@EventHandler
		public void onPlayerLogin(PlayerLoginEvent event) {
			if (Maximizer.coolDown) {
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Server is on kick cooldown. Try again in " + (Maximizer.countDown * 10) + " seconds.");
			} else {
				event.allow();
			}
			Maximizer.repeatDelay = 10;
		}
	}

}
