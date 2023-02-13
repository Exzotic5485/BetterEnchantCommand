package xyz.exzotic.betterenchantcommand.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemSlotArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Iterator;

public class DisenchantCommand {
    private static final DynamicCommandExceptionType FAILED_ENTITY_EXCEPTION = new DynamicCommandExceptionType((entityName) -> {
        return Text.translatable("commands.disenchant.failed.entity", new Object[]{entityName});
    });
    private static final DynamicCommandExceptionType FAILED_ITEMLESS_EXCEPTION = new DynamicCommandExceptionType((entityName) -> {
        return Text.translatable("commands.disenchant.failed.itemless", new Object[]{entityName});
    });
    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.disenchant.failed"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register((LiteralArgumentBuilder) ((LiteralArgumentBuilder) CommandManager.literal("disenchant").requires((source) -> {
            return source.hasPermissionLevel(2);
        })).then(CommandManager.argument("targets", EntityArgumentType.entities()).then(((RequiredArgumentBuilder) CommandManager.argument("enchantment", RegistryEntryArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT)).executes((context) -> {
            return execute((ServerCommandSource) context.getSource(), EntityArgumentType.getEntities(context, "targets"), RegistryEntryArgumentType.getEnchantment(context, "enchantment"), -1);
        })).then(CommandManager.argument("level", IntegerArgumentType.integer(0)).executes((context) -> {
            return execute((ServerCommandSource) context.getSource(), EntityArgumentType.getEntities(context, "targets"), RegistryEntryArgumentType.getEnchantment(context, "enchantment"), IntegerArgumentType.getInteger(context, "level"));
        }).then(CommandManager.argument("slot", ItemSlotArgumentType.itemSlot()).executes((context) -> {
            return executeSlotDisenchant((ServerCommandSource) context.getSource(), EntityArgumentType.getEntities(context, "targets"), RegistryEntryArgumentType.getEnchantment(context, "enchantment"), IntegerArgumentType.getInteger(context, "level"), ItemSlotArgumentType.getItemSlot(context, "slot"));
        }))).then(CommandManager.argument("slot", ItemSlotArgumentType.itemSlot()).executes((context) -> {
            return executeSlotDisenchant((ServerCommandSource) context.getSource(), EntityArgumentType.getEntities(context, "targets"), RegistryEntryArgumentType.getEnchantment(context, "enchantment"), -1, ItemSlotArgumentType.getItemSlot(context, "slot"));
        }).then(CommandManager.argument("level", IntegerArgumentType.integer(0)).executes((context) -> {
            return execute((ServerCommandSource) context.getSource(), EntityArgumentType.getEntities(context, "targets"), RegistryEntryArgumentType.getEnchantment(context, "enchantment"), IntegerArgumentType.getInteger(context, "level"));
        }))))));
    }

    private static int execute(ServerCommandSource source, Collection<? extends Entity> targets, RegistryEntry<Enchantment> enchantment, int level) throws CommandSyntaxException {
        Enchantment enchantment2 = enchantment.value();

        int i = 0;
        Iterator var6 = targets.iterator();

        while (var6.hasNext()) {
            Entity entity = (Entity) var6.next();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                ItemStack itemStack = livingEntity.getMainHandStack();
                if (!itemStack.isEmpty()) {
                    removeEnchantment(itemStack, enchantment2, level);
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
            source.sendFeedback(Text.translatable("commands.disenchant.success.single", new Object[]{enchantment2.getName(level), ((Entity) targets.iterator().next()).getDisplayName()}), true);
        } else {
            source.sendFeedback(Text.translatable("commands.disenchant.success.multiple", new Object[]{enchantment2.getName(level), targets.size()}), true);
        }

        return i;
    }

    private static int executeSlotDisenchant(ServerCommandSource source, Collection<? extends Entity> targets, RegistryEntry<Enchantment> enchantment, int level, int slot) throws CommandSyntaxException {
        Enchantment enchantment2 = enchantment.value();

        int i = 0;
        Iterator var6 = targets.iterator();

        while (var6.hasNext()) {
            Entity entity = (Entity) var6.next();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;

                StackReference stackReference = entity.getStackReference(slot);
                if (stackReference != StackReference.EMPTY) {
                    ItemStack itemStack = stackReference.get();
                    removeEnchantment(itemStack, enchantment2, level);
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
            source.sendFeedback(Text.translatable("commands.disenchant.success.single", new Object[]{enchantment2.getName(level), ((Entity) targets.iterator().next()).getDisplayName()}), true);
        } else {
            source.sendFeedback(Text.translatable("commands.disenchant.success.multiple", new Object[]{enchantment2.getName(level), targets.size()}), true);
        }

        return i;
    }

    private static void removeEnchantment(ItemStack itemStack, Enchantment enchantment, int level) {
        itemStack.getOrCreateNbt();

        if (!itemStack.getNbt().contains("Enchantments", 9)) {
            return;
        }

        NbtList nbtList = itemStack.getNbt().getList("Enchantments", 10);
        if (level == -1) {
            nbtList.removeIf((nbtElement) -> {
                return nbtElement instanceof NbtCompound && ((NbtCompound) nbtElement).getString("id").equals(Registries.ENCHANTMENT.getId(enchantment).toString());
            });
        } else {
            nbtList.remove(EnchantmentHelper.createNbt(EnchantmentHelper.getEnchantmentId(enchantment), (byte)level));
        }

        if (nbtList.isEmpty()) {
            itemStack.getNbt().remove("Enchantments");
        }
    }
}
