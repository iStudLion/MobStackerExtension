package aw.rmjtromp.runicextension.mobstacker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

import aw.rmjtromp.RunicCore.core.Core;
import aw.rmjtromp.RunicCore.utilities.RunicExtension;
import aw.rmjtromp.RunicCore.utilities.placeholders.Placeholder;
import net.minecraft.server.v1_8_R3.EntityLiving;

public final class MobStacker extends RunicExtension implements Listener {

	@Override
	public String getName() {
		return "MobStacker";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	public static MobStacker init() {
		return new MobStacker();
	}
	
	@Override
	public void onEnable() {
		//
	}
	
	private String nametag;
	private int maxStackSize = 25000;
	private List<EntityType> excludedEntities = new ArrayList<EntityType>();

	@Override
	public void loadConfigurations() {
		nametag = Core.getConfig().getString("extensions.mobstacker.nametag", "&6&lx{COUNT} &e&l{ENTITY}");
		maxStackSize = Core.getConfig().getInt("extensions.mobstacker.max-size", 25000);
		if(excludedEntities == null) excludedEntities = new ArrayList<EntityType>();
		if(excludedEntities.size() > 0) excludedEntities.clear();
		List<String> exclude = Core.getConfig().getStringList("extensions.mobstacker.exclude", Arrays.asList("irongolem", "endermen", "creeper", "villager", "wither", "enderdragon"));
		for(String entity : exclude) {
			EntityType e = plugin.getLibrary().getEntityType(entity);
			if(e != null && !excludedEntities.contains(e)) excludedEntities.add(e);
			else if(e == null) System.out.print("Invalid entity \""+entity+"\" at features.mobstacker.exclude inside config.yml");
		}
	}
	
//	@EventHandler
//	public void onSpawnerSpawn(SpawnerSpawnEvent e) {
//		if(excludedEntities.contains(e.getEntityType())) return;
//		recombineEntity(e.getEntity());
//		e.setCancelled(true);
//	}
//	
//	Above method is being handled below
	
	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent e) {
		if(excludedEntities.contains(e.getEntityType())) return;
		if(!e.getSpawnReason().equals(SpawnReason.SPAWNER_EGG) && !e.getSpawnReason().equals(SpawnReason.CUSTOM)) {
			recombineEntity(e.getEntity());
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityTame(EntityTameEvent e) {
		int size = e.getEntity().hasMetadata("size") ? (Integer) e.getEntity().getMetadata("size").get(0).value() : 1;
		size = size - 1 > 0 ? size - 1 : 1;
		if(e.getEntity().hasMetadata("size")) {
			e.getEntity().removeMetadata("size", plugin);
			e.getEntity().setCustomName("");
			setAI(e.getEntity(), true);
		}
		
		
		if(size >= 1) {
			Entity newEntity = e.getEntity().getLocation().getWorld().spawnEntity(e.getEntity().getLocation(), e.getEntityType());
			setAI(newEntity, false);
			newEntity.setMetadata("size", new FixedMetadataValue(plugin, size));
			newEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", size+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(e.getEntityType())).getString()));
		}
	}
	
	/*
	 * This stops players from convertig a *possibly huge* pig stack into a pigman stack for gold
	 */
	@EventHandler
	public void onPigZap(PigZapEvent e) {
		if(excludedEntities.contains(EntityType.PIG_ZOMBIE)) return;
		int size = e.getEntity().hasMetadata("size") ? (Integer) e.getEntity().getMetadata("size").get(0).value() : 1;
		size = size - 1 > 0 ? size - 1 : 1;
		if(size >= 1) {
			// resummons the pigstack
			Entity newEntity = e.getEntity().getLocation().getWorld().spawnEntity(e.getEntity().getLocation(), e.getEntityType());
			setAI(newEntity, false);
			newEntity.setMetadata("size", new FixedMetadataValue(plugin, size));
			newEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", size+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(e.getEntityType())).getString()));
		}
		
		// sets the pigmans name and size, and tries to combine with nearby pigmans
		if(e.getEntity().hasMetadata("size")) {
			e.getEntity().removeMetadata("size", plugin);
			e.getEntity().setCustomName(""); // remove name
			// the pigman stacking is being handled by creature spawn event
		}
	}
	
	@EventHandler
	public void onCreeperPower(CreeperPowerEvent e) {
		if(excludedEntities.contains(EntityType.CREEPER)) return;
		int size = e.getEntity().hasMetadata("size") ? (Integer) e.getEntity().getMetadata("size").get(0).value() : 1;
		size = size - 1 > 0 ? size - 1 : 1;
		if(size >= 1) {
			// resummons the creeper stack
			Entity newEntity = e.getEntity().getLocation().getWorld().spawnEntity(e.getEntity().getLocation(), e.getEntityType());
			setAI(newEntity, false);
			newEntity.setMetadata("size", new FixedMetadataValue(plugin, size));
			newEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", size+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(e.getEntityType())).getString()));
		}
		
		// removes size attribute and name
		if(e.getEntity().hasMetadata("size")) {
			e.getEntity().removeMetadata("size", plugin);
			e.getEntity().setCustomName(""); // remove name
			// the creeper stacking is being handled by creature spawn event
		}
	}
	
	@EventHandler
	public void PlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
		if(e.getPlayer().getItemInHand().getType().equals(Material.SADDLE)) {
			Entity entity = e.getRightClicked();
			if(entity == null) return;
			if(entity.getType().equals(EntityType.HORSE) || entity.getType().equals(EntityType.PIG)) {
				int size = entity.hasMetadata("size") ? (Integer) entity.getMetadata("size").get(0).value() : 1;
				size = size - 1 > 0 ? size - 1 : 0;
				if(entity.hasMetadata("size")) {
					entity.removeMetadata("size", plugin);
					entity.setCustomName("");
					setAI(entity, false);
				}
				
				if(size >= 1) {
					Entity newEntity = entity.getLocation().getWorld().spawnEntity(entity.getLocation(), entity.getType());
					setAI(newEntity, false);
					newEntity.setMetadata("size", new FixedMetadataValue(plugin, size));
					newEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", size+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(newEntity)).getString()));
				}
			}
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {	
		if(excludedEntities.contains(e.getEntityType())) return;
		if(e.getEntity().getKiller() != null) {
			if(!(e.getEntity() instanceof Player) && e.getEntity().getKiller() instanceof Player && e.getEntity().hasMetadata("size")) {
				int amount = e.getEntity().hasMetadata("size") ? (Integer) e.getEntity().getMetadata("size").get(0).value() : 1;
				
				Player player = (Player) e.getEntity().getKiller();
				int sharpnessLevel = 1;
				int lootingLevel = 0;
				
				if(!player.getItemInHand().equals(null) && !player.getItemInHand().getType().equals(Material.AIR)) {
					if(player.getItemInHand().containsEnchantment(Enchantment.DAMAGE_ALL)) {
						sharpnessLevel = (player.getItemInHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL) < 1) ? 1 : player.getItemInHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL);
					}
					
					if(player.getItemInHand().containsEnchantment(Enchantment.LOOT_BONUS_MOBS)) {
						lootingLevel = player.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS);
					}
				}
				
				int newAmount = amount - sharpnessLevel;
				int amountKilled = (amount >= sharpnessLevel) ? sharpnessLevel : amount;
				if(newAmount > 0) {
					Entity newEntity = e.getEntity().getWorld().spawnEntity(e.getEntity().getLocation(), e.getEntityType());
					setAI(newEntity, false);
					newEntity.setMetadata("size", new FixedMetadataValue(plugin, newAmount));
					newEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", newAmount+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(e.getEntityType())).getString()));
				}
				
//				e.getEntity().setVelocity();
				
				List<org.bukkit.inventory.ItemStack> drops = e.getDrops();
				for(int i = 0; i < drops.size(); i++) {
					/**
					 * TODO FIX DUPING GLITCH WITH ENTITIES DROPPING
					 * Attempting to prevent duping glitch by skipping anything with saddle, armor, and chest
					 */
					if(drops.get(i).getType().toString().toLowerCase().contains("armor") || drops.get(i).getType().toString().toLowerCase().contains("chest") || drops.get(i).getType().equals(Material.SADDLE)) continue;
					int dropAmount = drops.get(i).getAmount();
					if(lootingLevel > 0) e.getDrops().get(i).setAmount(dropAmount * lootingLevel * amountKilled);
					else e.getDrops().get(i).setAmount(dropAmount * amountKilled);
				}
				
				if(lootingLevel > 0) e.setDroppedExp(e.getDroppedExp() * lootingLevel * amountKilled);
				else e.setDroppedExp(e.getDroppedExp() * amountKilled);
				return;
			}
		} else if(e.getEntity().hasMetadata("size")) {
			if(e.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE) || e.getEntity().getLastDamageCause().getCause().equals(DamageCause.FIRE_TICK) || e.getEntity().getLastDamageCause().getCause().equals(DamageCause.LIGHTNING)) {
				int amount = e.getEntity().hasMetadata("size") ? (Integer) e.getEntity().getMetadata("size").get(0).value() : 1;
				amount--;
				
				if(amount > 0) {
					Entity newEntity = e.getEntity().getWorld().spawnEntity(e.getEntity().getLocation(), e.getEntityType());
					
					// make the entity adult
					if(newEntity instanceof Ageable && !((Ageable) newEntity).isAdult()) ((Ageable) newEntity).setAdult();
					
					setAI(newEntity, false);
					newEntity.setMetadata("size", new FixedMetadataValue(plugin, amount));
					newEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", amount+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(e.getEntityType())).getString()));
				}
			}
		}
	}
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent e) {
		if(e.getEntity().hasMetadata("size")) e.setCancelled(true);
	}
	
	@EventHandler
	public void onSlimeSplit(SlimeSplitEvent e) {
//		recombineEntity(e.getEntity()); // right?
		// being handled by creature spawn event
	}
	
	private void setAI(LivingEntity entity, boolean enabled) {
		  EntityLiving handle = ((CraftLivingEntity) entity).getHandle();
		  handle.getDataWatcher().watch(15, (byte) (enabled ? 0 : 1));
	}
	
	private void setAI(Entity entity, boolean enabled) {
		if(entity instanceof LivingEntity) setAI((LivingEntity) entity, enabled);
	}
	
	private boolean hasAI(LivingEntity entity) {
		  EntityLiving handle = ((CraftLivingEntity) entity).getHandle();
		  byte b = handle.getDataWatcher().getByte(15);
		  if(b == (byte)1) return false;
		  return true;
	}
	
	private boolean hasAI(Entity entity) {
		if(entity instanceof LivingEntity) return hasAI((LivingEntity)entity);
		return false;
	}
	
	private void recombineEntity(Entity entity) {
		List<Entity> entities = entity.getNearbyEntities(16, 16, 16);
//		Entity[] entities = Core.getNearbyEntities(entity.getLocation(), 16);
		
		ArrayList<Entity> ent = new ArrayList<Entity>();
		for(int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if(e.getType().equals(entity.getType())) {
				if(e.isDead()) continue;
				if(e.getType().equals(EntityType.WOLF)) if(((Wolf) e).isTamed()) continue;
				if(e.getType().equals(EntityType.OCELOT)) if(((Ocelot) e).isTamed()) continue;
				if(e.getType().equals(EntityType.HORSE)) if(((Horse) e).isTamed()) continue;
				if(e.getType().equals(EntityType.CREEPER)) if(((Creeper) e).isPowered()) continue;
				if(e.getType().equals(EntityType.PIG)) if(((Pig) e).hasSaddle()) continue;
				
				ent.add(e);
			}
		}
		
		if(!ent.isEmpty()) {
			if(ent.size() > 1) {
				Entity MainEntity = null;
				int MainEntitySize = 0;
				int totalSize = 0;
				
				// Get the biggest entity stack and total size, and 'restack'
				for(Entity e : ent) {
					int EntitySize = e.hasMetadata("size") ? (Integer) e.getMetadata("size").get(0).value() : 1;
					if(MainEntity == null) {
						MainEntity = e;
						MainEntitySize = MainEntity.hasMetadata("size") ? (Integer) MainEntity.getMetadata("size").get(0).value() : 1;
					} else {
						if(EntitySize > MainEntitySize) MainEntity = e;
					}
					totalSize += EntitySize;
				}
				
				// Kill all stacks besides the biggest stack and 'merge' all other stacks with biggest stack
				for(Entity e : ent) if(!entity.equals(MainEntity)) e.remove();
				
				if(hasAI(MainEntity)) setAI(MainEntity, false);
				
				// update the new Main entity's size and nametag
				if(MainEntity.hasMetadata("size")) MainEntity.removeMetadata("size", plugin);
				
				int FinalSize = totalSize > maxStackSize ? maxStackSize : totalSize < 1 ? 1 : totalSize;
				MainEntity.setMetadata("size", new FixedMetadataValue(plugin, FinalSize));
				MainEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", FinalSize+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(MainEntity)).getString()));
			} else {
				// get stack size
				Entity e = ent.get(0);
				int size = e.hasMetadata("size") ? e.getMetadata("size").get(0).asInt() : 1;
				
				// if size is smaller than limit, continue stacking
				if(size < maxStackSize) {
					// remove "size" metadata
					if(e.hasMetadata("size")) e.removeMetadata("size", plugin);
					
					if(size + 1 >= maxStackSize) {
						// if new size is bigger than limit, set size to limit
						e.setMetadata("size", new FixedMetadataValue(plugin, maxStackSize));
						e.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", maxStackSize+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(entity)).getString()));
					} else {
						// set size to new size
						e.setMetadata("size", new FixedMetadataValue(plugin, size+1));
						e.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", size+1+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(entity)).getString()));
					}
				}
			}
		} else {
			Entity e = entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());
			setAI(e, false);
			e.setMetadata("size", new FixedMetadataValue(plugin, 1));
			e.setCustomName(ChatColor.translateAlternateColorCodes('&', Placeholder.parse(nametag).set("{COUNT}", 1+"").set("{ENTITY}", plugin.getLibrary().getFriendlyName(e)).getString()));
		}
		if(entity != null || !entity.isDead()) entity.remove();
	}

}
