package com.pppopipupu.combp.mixin;

import net.minecraft.command.CommandEnchant;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CommandEnchant.class)
public abstract class MixinCommandEnchant {
    @Redirect(method = "Lnet/minecraft/command/CommandEnchant;processCommand(Lnet/minecraft/command/ICommandSender;[Ljava/lang/String;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/Enchantment;canApply(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean canApply(Enchantment enchantment, ItemStack itemstack) {
        return true;
    }

    @Redirect(method = "Lnet/minecraft/command/CommandEnchant;processCommand(Lnet/minecraft/command/ICommandSender;[Ljava/lang/String;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/Enchantment;getMaxLevel()I"))
    private int getMaxLevel(Enchantment enchantment) {
        return 255;
    }

    @Redirect(method = "Lnet/minecraft/command/CommandEnchant;processCommand(Lnet/minecraft/command/ICommandSender;[Ljava/lang/String;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/Enchantment;canApplyTogether(Lnet/minecraft/enchantment/Enchantment;)Z"))
    private boolean canCombine(Enchantment enchantment1,Enchantment enchantment2) {
        return true;
    }



}
