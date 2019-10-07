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

package pl.asie.debark.messy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import pl.asie.debark.old.UCWColorspaceUtils;

import java.util.Collection;
import java.util.function.Function;

public class LogColoredSprite extends TextureAtlasSprite {
    private final ResourceLocation base;
    private final ResourceLocation template;

    public LogColoredSprite(String spriteName, ResourceLocation base, ResourceLocation template) {
        super(spriteName);
        this.base = base;
        this.template = template;
    }

    @Override
    public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location) {
        return true;
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return ImmutableSet.of(base, template);
    }

    @Override
    public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        TextureAtlasSprite baseTex = textureGetter.apply(base);
        TextureAtlasSprite templateTex = textureGetter.apply(template);

        float minL = Float.MAX_VALUE;
        float maxL = Float.MIN_VALUE;
        double A = 0;
        double B = 0;
        int count = 0;

        int offset = (baseTex.getIconWidth() + 7) / 8;

        for (int iy = offset; iy < baseTex.getIconHeight() - offset; iy++) {
            for (int ix = offset; ix < baseTex.getIconWidth() - offset; ix++) {
                int pixel = baseTex.getFrameTextureData(0)[0][iy * baseTex.getIconWidth() + ix];
                float[] lab = UCWColorspaceUtils.XYZtoLAB(UCWColorspaceUtils.sRGBtoXYZ(UCWColorspaceUtils.fromInt(pixel)));
                if (lab[0] < minL) minL = lab[0];
                if (lab[0] > maxL) maxL = lab[0];
                A += lab[1]; B += lab[2];
                count++;
            }
        }

        assert count >= 1;
        A /= count;
        B /= count;

        // recolor template texture
        int[][] templateData = new int[Minecraft.getMinecraft().getTextureMapBlocks().getMipmapLevels() + 1][];
        templateData[0] = new int[templateTex.getIconWidth() * templateTex.getIconHeight()];
        for (int i = 0; i < templateData[0].length; i++) {
            int oldPixel = templateTex.getFrameTextureData(0)[0][i];
            float l = UCWColorspaceUtils.sRGBtoLuma(UCWColorspaceUtils.fromInt(oldPixel));
            l = ((l / 100f) * (maxL - minL)) + minL;
            float[] lab = new float[] { l, (float) A, (float) B };

            templateData[0][i] = UCWColorspaceUtils.asInt(UCWColorspaceUtils.XYZtosRGB(UCWColorspaceUtils.LABtoXYZ(lab))) | 0xFF000000;
        }

        setIconWidth(templateTex.getIconWidth());
        setIconHeight(templateTex.getIconHeight());
        setFramesTextureData(ImmutableList.of(templateData));

        return false;
    }
}
