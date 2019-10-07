/*
 * Copyright (c) 2017, 2018, 2019 Adrian Siekierka
 *
 * This file is part of Debark.
 *
 * Debark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Debark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Debark.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.debark.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import pl.asie.debark.DebarkMod;

import java.lang.reflect.Field;
import java.util.Map;

public class ModelLoaderEarlyView {
    private ModelLoader loader;
    private Map<ModelResourceLocation, IModel> secretSauce = null;
    private BlockModelShapes blockModelShapes = null;

    public ModelLoaderEarlyView() {
        // don't tell lex
        try {
            Class c = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaLoader");
            Field f = c.getDeclaredField("INSTANCE");
            f.setAccessible(true);
            Object o = f.get(null);
            f = c.getDeclaredField("loader");
            f.setAccessible(true);
            loader = (ModelLoader) f.get(o);
            f = ModelLoader.class.getDeclaredField("stateModels");
            f.setAccessible(true);
            secretSauce = (Map<ModelResourceLocation, IModel>) f.get(loader);
            f = ObfuscationReflectionHelper.findField(ModelBakery.class, "field_177610_k");
            f.setAccessible(true);
            blockModelShapes = (BlockModelShapes) f.get(loader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public IModel getModel(IBlockState state) {
        Map<IBlockState, ModelResourceLocation> variants = blockModelShapes.getBlockStateMapper().getVariants(state.getBlock());

        if (variants != null) {
            ModelResourceLocation loc = variants.get(state);
            if (loc != null) {
                return secretSauce.get(loc);
            }
        }

        return null;
    }
}
