package com.github.sculkhorde.common.item;

import com.github.sculkhorde.core.SculkHorde;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.extensions.IForgeItem;

public class DevNodeSpawner extends Item implements IForgeItem {

	/**
	 * The Constructor that takes in properties
	 * @param properties The Properties
	 */
	public DevNodeSpawner(Properties properties) {
		super(properties);

	}

	/**
	 * A simpler constructor that does not take in properties.<br>
	 * I made this so that registering items in ItemRegistry.java can look cleaner
	 */
	public DevNodeSpawner() {this(getProperties());}

	/**
	 * Determines the properties of an item.<br>
	 * I made this in order to be able to establish a item's properties from within the item class and not in the ItemRegistry.java
	 * @return The Properties of the item
	 */
	public static Properties getProperties()
	{
		return new Properties()
				.tab(SculkHorde.SCULK_GROUP)
				.rarity(Rarity.EPIC)
				.fireResistant();

	}

	@Override
	public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn)
	{
		ItemStack itemstack = playerIn.getItemInHand(handIn);

		//If item is not on cool down
		if(playerIn.getCooldowns().isOnCooldown(this) || worldIn.isClientSide())
		{
			return ActionResult.fail(itemstack);
		}


		SculkHorde.gravemind.placeSculkNode((ServerWorld) worldIn, playerIn.blockPosition(), false);
		return ActionResult.pass(itemstack);
	}
}