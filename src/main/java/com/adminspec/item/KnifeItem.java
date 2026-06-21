package com.adminspec.item;

import com.adminspec.entity.KnifeProjectileEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class KnifeItem extends Item {
    public KnifeItem(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 7200;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;
        int charge = getUseDuration(stack, user) - remainingUseTicks;
        float speed = Math.min(charge / 10f, 1.5f);
        if (!level.isClientSide) {
            player.getCooldowns().addCooldown(this, 15);
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            KnifeProjectileEntity knife = new KnifeProjectileEntity(level, player);
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f * speed, 1f);
            level.addFreshEntity(knife);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5f, 1f);
        }
    }
}
