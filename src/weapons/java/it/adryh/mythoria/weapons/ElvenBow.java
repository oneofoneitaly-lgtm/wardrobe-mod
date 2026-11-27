package it.adryh.mythoria.weapons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A fully drawn shot sends two extra elven arrows alongside the real one. */
public final class ElvenBow extends BowItem {
    public ElvenBow(Properties properties) {
        super(properties);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity shooter, int timeLeft) {
        boolean full = shooter instanceof Player player && !player.getProjectile(stack).isEmpty()
                && getPowerForTime(getUseDuration(stack, shooter) - timeLeft) >= 1.0F;
        // copied before the shot: the bow may break on this very arrow
        ItemStack bow = stack.copy();
        super.releaseUsing(stack, level, shooter, timeLeft);
        if (full && level instanceof ServerLevel server) {
            for (float spread : new float[]{-9.0F, 9.0F}) {
                Arrow arrow = new Arrow(level, shooter, new ItemStack(Items.ARROW), bow);
                arrow.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + spread, 0.0F, 3.0F, 0.6F);
                arrow.setCritArrow(true);
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                level.addFreshEntity(arrow);
            }
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, shooter.getX(), shooter.getEyeY() - 0.2, shooter.getZ(), 18, 0.5, 0.3, 0.5, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythoria_weapons.elven_bow.hit").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.mythoria_weapons.elven_bow.use").withStyle(ChatFormatting.AQUA));
    }
}
