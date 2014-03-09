/******************************************
 *            COM: Zombies                *
 * Developers: Connor Hollasch, Ryan Turk *
 *****************************************/

package com.zombies.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.zombies.COMZombies;
import com.zombies.CommandUtil;
import com.zombies.Arena.Game;
import com.zombies.Arena.GameManager;
import com.zombies.InGameFeatures.Features.Door;
import com.zombies.Spawning.SpawnPoint;

public class OnBlockBreakEvent implements Listener
{

	private COMZombies plugin;
	private GameManager manager;

	public OnBlockBreakEvent(COMZombies z)
	{
		plugin = z;
		manager = z.manager;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBreakEvent(BlockBreakEvent interact)
	{
		Player player = interact.getPlayer();
		if (plugin.isRemovingDoors.containsKey(player))
		{
			Game game = plugin.isRemovingDoors.get(player);
			Location loc = interact.getBlock().getLocation();
			Door door = game.getInGameManager().getDoorFromSign(loc);
			if (door == null) return;
			door.removeSelfFromConfig();
			interact.setCancelled(true);
			for (final Sign sign : door.getSigns())
			{
				boom(sign);
			}
			CommandUtil.sendMessageToPlayer(player, ChatColor.GREEN + "" + ChatColor.BOLD + "Door removed!");
			game.getInGameManager().removeDoor(door);
			if (game.getInGameManager().getDoors().size() == 0)
			{
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "No doors left!");
				String[] args = new String[2];
				args[0] = "cancel";
				args[1] = "removedoor";
				plugin.command.onRemoteCommand(player, args);
			}
		}
		if (plugin.isRemovingSpawns.containsKey(player))
		{
			Game game = plugin.isRemovingSpawns.get(player);
			for (SpawnPoint point : game.spawnManager.getPoints())
			{
				if (interact.getBlock().getLocation().equals(point.getLocation()))
				{
					game.spawnManager.removePoint(player, point);
					interact.setCancelled(false);
					if (game.spawnManager.getPoints().size() == 0)
					{
						CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "No spawns left! Force canceling this operation!");
						String[] args = new String[2];
						args[0] = "cancel";
						args[1] = "removespawn";
						plugin.command.onRemoteCommand(player, args);
					}
					return;
				}
			}
		}
		if (manager.isPlayerInGame(player) == true)
		{
			interact.setCancelled(true);
			return;
		}
		try
		{
			if (manager.isLocationInGame(interact.getBlock().getLocation()))
			{
				interact.setCancelled(true);
				return;
			}
		} catch (Exception e)
		{
			return;
		}
		if (interact.getBlock().getType().getId() == Material.WALL_SIGN.getId() || interact.getBlock().getType().getId() == Material.SIGN.getId() || interact.getBlock().getType().getId() == Material.SIGN_POST.getId())
		{
			Sign sign = (Sign) interact.getBlock().getState();
			String lineOne = sign.getLine(0);
			String lineTwo = sign.getLine(1);
			if(ChatColor.stripColor(lineOne).equalsIgnoreCase("[Zombies]") && ChatColor.stripColor(lineTwo).equalsIgnoreCase("MysteryBox"))
			{
				Game game = plugin.manager.getGame(interact.getBlock().getLocation());
				if(game!=null)
				{
					game.boxManager.removeBox(interact.getPlayer(), game.boxManager.getBox(sign.getLocation()));
				}
			}
		}
	}

	public void boom(final Sign sign)
	{
		int j = 1;
		for (int i = 6; i > 0; i--)
		{

			final int copyI = (i - 1);
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
			{
				@Override
				public void run()
				{
					if (copyI < 1)
					{
						sign.getLocation().getBlock().setType(Material.AIR);
						sign.getWorld().playSound(sign.getLocation(), Sound.EXPLODE, 1, 1);
						sign.getWorld().playEffect(sign.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
					}
					else
					{
						sign.setLine(0, "");
						sign.setLine(1, ChatColor.RED + "Removing in:");
						sign.setLine(2, Integer.toString(copyI));
						sign.setLine(3, "");
						sign.update();
						sign.update(true);
					}
				}

			}, j * 20);
			j += 1;
		}
	}
}
