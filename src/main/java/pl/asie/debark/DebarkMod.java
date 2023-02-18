/*
 * Copyright (c) 2017, 2018, 2019 Adrian Siekierka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pl.asie.debark;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.SlotFurnaceFuel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemMultiTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.Logger;
import pl.asie.debark.util.BlockStateIterator;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod(modid = DebarkMod.MODID, version = DebarkMod.VERSION,
        dependencies = "after:forge@[14.23.5.2838,)",
        updateJSON = "http://asie.pl/files/minecraft/update/" + DebarkMod.MODID + ".json")
public final class DebarkMod {
    public static final String MODID = "debark";
    public static final String VERSION = "${version}";
    public static Logger logger;
    private static Configuration config;

    @SidedProxy(modId = DebarkMod.MODID, clientSide = "pl.asie.debark.DebarkProxyClient", serverSide = "pl.asie.debark.DebarkProxyCommon")
    private static DebarkProxyCommon proxy;

    private static final Map<String, DebarkBlockEntry> entries = new LinkedHashMap<>();
    static final Map<IBlockState, BlockDebarkedLogEntry> blocksMap = new LinkedHashMap<>();

    private static boolean debarkByRecipe, debarkInWorld, debarkInWorldRequiresShift;
    public static boolean enableDebugLogging;

    private void add(String key) {
        String[] keySplit = key.split(",");
        String[] keySplitArgs = new String[keySplit.length - 1];
        System.arraycopy(keySplit, 1, keySplitArgs, 0, keySplitArgs.length);
        entries.put(keySplit[0], new DebarkBlockEntry(keySplit[0], keySplitArgs));
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        logger = event.getModLog();

        if (Loader.isModLoaded("debarkedlogs")) {
            logger.info("Beeto's Debarked Logs detected - disabling vanilla debarked logs!");
        } else {
            add("minecraft:log,variant");
            add("minecraft:log2,variant");
        }
        for (int i = 0; i <= 7; i++) {
            add("forestry:logs." + i + ",variant");
            add("forestry:logs.fireproof." + i + ",variant");
        }
        // add("forestry:logs.vanilla.fireproof.0,variant"); // bug: invalid textures
        // add("forestry:logs.vanilla.fireproof.1,variant"); // bug: invalid textures
        for (int i = 0; i <= 9; i++) {
            add("extratrees:logs." + i + ",variant");
            add("extratrees:logs.fireproof." + i + ",variant");
        }
        for (int i = 0; i <= 4; i++) {
            add("biomesoplenty:log_" + i + ",variant");
        }
        add("aether_legacy:aether_log,aether_logs");
        add("atum:palm_log");
        add("atum:deadwood_log");
        add("bewitchment:juniper_wood");
        add("bewitchment:elder_wood");
        add("bewitchment:yew_wood");
        add("bewitchment:cypress_wood");
        add("climaticbiomesjbg:pine_log");
        add("natura:overworld_logs,type");
        add("natura:overworld_logs2,type");
        add("natura:nether_logs,type");
        add("pvj:log_aspen");
        add("pvj:log_baobab");
        add("pvj:log_cherry_blossom");
        add("pvj:log_cottonwood");
        add("pvj:log_fir");
        add("pvj:log_jacaranda");
        add("pvj:log_juniper");
        add("pvj:log_mangrove");
        add("pvj:log_maple");
        add("pvj:log_palm");
        add("pvj:log_pine");
        add("pvj:log_redwood");
        add("pvj:log_willow");
        add("rockhounding_surface:bog_logs,variant");
        add("rockhounding_surface:cold_logs,variant");
        add("rockhounding_surface:fossil_logs,variant");
        add("rockhounding_surface:petrified_logs,variant");
        add("rustic:log,variant");
        for (String s : new String[]{"acacia", "ash", "aspen", "birch", "blackwood", "chestnut", "douglas_fir",
                                     "hickory", "kapok", "maple", "oak", "palm", "pine", "rosewood", "sequoia",
                                     "spruce", "sycamore", "white_cedar", "willow"}) {
            add("tfc:wood/log/" + s);
        }
        add("traverse:fir_log");
        add("twilightforest:twilight_log,variant");
        add("ic2:rubber_wood");
        add("mysticalworld:charred_log");
        add("extraplanets:kepler22b_maple_logs,variant");
        add("extraplanets:kepler22b_maple_logs2,variant");

        String[] edl = config.get("modsupport", "extraDebarkedLogs", new String[0], "Format: blockId,property1,property2,etc").getStringList();
        Stream.of(edl).forEach(this::add);

        debarkByRecipe = config.getBoolean("debarkByRecipe", "interactions", true, "Allow debarking in crafting tables.");
        debarkInWorld = config.getBoolean("debarkInWorld", "interactions", true, "Allow debarking by right-clicking blocks with an axe.");
        debarkInWorldRequiresShift = config.getBoolean("debarkInWorldRequiresShift", "interactions", false, "Require shift-right-clicking for debarking in-world.");
        enableDebugLogging = config.getBoolean("debugLogsEnabled", "debug", false, "Enable debug logs.");

        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit();

        if (config.hasChanged()) {
            config.save();
        }
    }

    @Mod.EventHandler
    public void onInterModComms(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            if ("register_log".equals(message.key) && message.isStringMessage()) {
                if (!blocksMap.isEmpty()) {
                    logger.warn("Warning: IMC message " + message.key + "(" + message.getStringValue() + ") from " + message.getSender() + " was sent too late!");
                }
                add(message.getStringValue());
            } else {
                logger.warn("Unknown IMC message: " + message.key + " from " + message.getSender());
            }
        }
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        for (BlockDebarkedLogEntry block : blocksMap.values()) {
            ItemStack stack = block.getItemStack();
            OreDictionary.registerOre("debarkedLogWood", stack);
        }
    }

    public static IBlockState debarkedLogFor(IBlockState state) {
        IBlockState stateKey = BlockStateIterator.permuteByNames(state, ImmutableList.of("axis")).filter(blocksMap::containsKey).findFirst().orElse(null);
        if (stateKey != null) {
            BlockDebarkedLogEntry targetBlock = blocksMap.get(stateKey);
            String axisValue = null;
            for (IProperty property : state.getPropertyKeys()) {
                if ("axis".equals(property.getName())) {
                    axisValue = property.getName(state.getValue(property));
                    break;
                }
            }
            if (axisValue != null) {
                try {
                    BlockLog.EnumAxis enumAxis = BlockLog.EnumAxis.valueOf(axisValue.toUpperCase(Locale.ROOT));
                    IBlockState resultState = targetBlock.getBlockState().withProperty(BlockLog.LOG_AXIS, enumAxis);
                    return resultState;
                } catch (IllegalArgumentException e) {
                    // it fine
                }
            }
        }

        return null;
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (debarkInWorld) {
            if (debarkInWorldRequiresShift && !event.getEntityPlayer().isSneaking()) {
                return;
            }

            if (!stack.isEmpty() && stack.getItem().getToolClasses(stack).contains("axe")) {
                // we have an axe
                IBlockState state = event.getWorld().getBlockState(event.getPos());
                IBlockState debarkedState = debarkedLogFor(state);
                if (debarkedState != null) {
                    event.getWorld().setBlockState(event.getPos(), debarkedState, 3);
                    stack.onBlockDestroyed(event.getWorld(), state, event.getPos(), event.getEntityPlayer());
                    event.getEntityPlayer().swingArm(event.getHand());
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        blocksMap.clear();
        for (DebarkBlockEntry entry : entries.values()) {
            if (!event.getRegistry().containsKey(entry.getBlock())) {
                continue;
            }

            Block block = event.getRegistry().getValue(entry.getBlock());
            if (block == null || block.getRegistryName() == null) {
                continue;
            }

            IBlockState defaultState = block.getDefaultState();
            List<IBlockState> states = BlockStateIterator.permuteByNames(defaultState, entry.getProperties()).collect(Collectors.toList());
            if (states.size() > 4) {
                logger.warn("Could not properly handle " + entry.getBlock() + " - too many states!");
                // TODO: don't know what to do with this...
                continue;
            }

            BlockDebarkedLog debarkedBlock = new BlockDebarkedLog(states.toArray(new IBlockState[0]));
            for (int i = 0; i < states.size(); i++) {
                blocksMap.put(states.get(i), new BlockDebarkedLogEntry(debarkedBlock, i));
            }
            // TFC hack: placed=false shall be debarkable like placed=true
            if ("tfc".equals(block.getRegistryName().getNamespace())) {
                for (int i = 0; i < states.size(); i++) {
                    blocksMap.put(states.get(i).cycleProperty(block.getBlockState().getProperty("placed")), new BlockDebarkedLogEntry(debarkedBlock, i));
                }
            }
            debarkedBlock.setRegistryName("debark:debarked_log_" + block.getRegistryName().toString().replaceAll("[^A-Za-z0-9]", "_"));
            event.getRegistry().register(debarkedBlock);
        }
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        Set<Block> addedBlocks = new HashSet<>();

        blocksMap.values().forEach(blockEntry -> {
            if (!addedBlocks.add(blockEntry.getBlock())) {
                return;
            }

            ItemBlock itemBlock = new ItemBlock(blockEntry.getBlock()) {
                @Override
                public int getMetadata(int damage) {
                    return damage >= 0 && damage < ((BlockDebarkedLog) block).getVariantCount() ? damage : 0;
                }

                @Override
                public String getItemStackDisplayName(ItemStack stack) {
                    return ((BlockDebarkedLog) block).getLocalizedName(stack.getMetadata());
                }
            };

            itemBlock.setHasSubtypes(true);
            itemBlock.setRegistryName(blockEntry.getBlock().getRegistryName());
            event.getRegistry().register(itemBlock);
        });
    }

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (debarkByRecipe) {
            blocksMap.values().forEach(blockEntry -> {
                BlockDebarkedLog block = blockEntry.getBlock();
                int i = blockEntry.getVariant();
                ItemStack log = block.getParentStack(i);
                ItemStack debarkedLog = new ItemStack(block, 1, i);
                DebarkShapelessRecipe recipe = new DebarkShapelessRecipe(log, debarkedLog);
                recipe.setRegistryName(new ResourceLocation("debark", "debark_log_" + block.getRegistryName().getPath()));
                event.getRegistry().register(recipe);
            });
        }
    }
}
