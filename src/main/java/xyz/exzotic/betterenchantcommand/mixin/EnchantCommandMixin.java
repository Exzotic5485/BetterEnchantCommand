package xyz.exzotic.betterenchantcommand.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EnchantmentArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.EnchantCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Iterator;

@Mixin(EnchantCommand.class)
public class EnchantCommandMixin {
    private static int j = 0;

    @Shadow
    private static DynamicCommandExceptionType FAILED_ENTITY_EXCEPTION;

    @Shadow
    private static DynamicCommandExceptionType FAILED_ITEMLESS_EXCEPTION;
    @Shadow
    private static SimpleCommandExceptionType FAILED_EXCEPTION;

    private static int executeSlotEnchant(ServerCommandSource source, Collection<? extends Entity> targets, Enchantment enchantment, int level, int slot) throws CommandSyntaxException {
        int i = 0;
        Iterator var6 = targets.iterator();

        while (var6.hasNext()) {
            Entity entity = (Entity) var6.next();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                StackReference stackReference = entity.getStackReference(slot);
                if (stackReference != StackReference.EMPTY) {
                    ItemStack itemStack = stackReference.get();

                    itemStack.addEnchantment(enchantment, level);
                    ++i;
                } else if (targets.size() == 1) {
                    throw FAILED_ITEMLESS_EXCEPTION.create(livingEntity.getName().getString());
                }
            } else if (targets.size() == 1) {
                throw FAILED_ENTITY_EXCEPTION.create(entity.getName().getString());
            }
        }

        if (i == 0) {
            throw FAILED_EXCEPTION.create();
        }

        if (targets.size() == 1) {
            source.sendFeedback(new TranslatableText("commands.enchant.success.single", new Object[]{enchantment.getName(level), ((Entity) targets.iterator().next()).getDisplayName()}), true);
        } else {
            source.sendFeedback(new TranslatableText("commands.enchant.success.multiple", new Object[]{enchantment.getName(level), targets.size()}), true);
        }

        return i;
    }

    @ModifyExpressionValue(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"))
    private static ArgumentBuilder addArgument(ArgumentBuilder argumentBuilder) {
        if(j == 0) {
            j++;
            return argumentBuilder;
        }

        return argumentBuilder.then(CommandManager.argument("slot", ItemSlotArgumentType.itemSlot()).executes((context) -> {
            return executeSlotEnchant((ServerCommandSource) context.getSource(), EntityArgumentType.getEntities(context, "targets"), EnchantmentArgumentType.getEnchantment(context, "enchantment"), IntegerArgumentType.getInteger(context, "level"), ItemSlotArgumentType.getItemSlot(context, "slot"));
        }));
    }


    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/Enchantment;getMaxLevel()I"))
    private static int removeLimit(Enchantment instance) {
        return 255;
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/Enchantment;isAcceptableItem(Lnet/minecraft/item/ItemStack;)Z"))
    private static boolean allowEnchantOnAnyItem(Enchantment instance, net.minecraft.item.ItemStack stack) {
        return true;
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;isCompatible(Ljava/util/Collection;Lnet/minecraft/enchantment/Enchantment;)Z"))
    private static boolean removeEnchantCompatibilityProblems(Collection<Enchantment> existing, Enchantment candidate) {
        return true;
    }

}
