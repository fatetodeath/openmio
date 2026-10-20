package me.mioclient.module.player.cheststealer;

import me.mioclient.util.anim.AnimationProgress;
import me.mioclient.util.math.GridPoint;

import me.mioclient.api.ClientAccessor;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class ItemTransferAnimation implements ClientAccessor {
    public final AnimationProgress progress = new AnimationProgress(2.0f);
    public final ItemStack itemStack;
    public final GridPoint positionData;
    public final GridPoint positionData2;

    public ItemTransferAnimation(ItemStack itemStack, GridPoint positionData, GridPoint positionData2) {
        this.itemStack = itemStack.copy();
        this.positionData = positionData2;
        this.positionData2 = positionData;
        this.progress.setToggledImmediate(false);
    }

    public void do2548(GuiGraphicsExtractor drawContext) {
        this.progress.animateToggle(true);
        float f = this.progress.getValue();
        int i = (int) (this.positionData2.get476() + ((this.positionData.get476() - this.positionData2.get476()) * f));
        int i2 = (int) (this.positionData2.getGridY() + ((this.positionData.getGridY() - this.positionData2.getGridY()) * f));
        drawContext.pose().pushMatrix();
        // 26.2: RenderSystem.setShaderColor was removed; item fade tint has no pipeline hook.
        drawContext.item(this.itemStack, i, i2);
        drawContext.itemDecorations(minecraftClient.font, this.itemStack, i, i2);
        drawContext.pose().popMatrix();
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public GridPoint getTargetPosition() {
        return this.positionData;
    }

    public boolean is2551() {
        return this.progress.getRawValue() == 2.0f;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ItemTransferAnimation chestStealerSearchHelper4_2 = (ItemTransferAnimation) obj;
        return Objects.equals(this.itemStack.getItem(), chestStealerSearchHelper4_2.itemStack.getItem()) && Objects.equals(this.positionData, chestStealerSearchHelper4_2.positionData);
    }

    public int hashCode() {
        return Objects.hash(this.itemStack.getItem(), this.positionData);
    }
}