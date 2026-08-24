package com.rasmus.rarefishfinder.mixin;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementTabType;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import com.rasmus.rarefishfinder.gui.CollectionScreen;
import com.rasmus.rarefishfinder.gui.FishTabView;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fish Collection tab for users running Better Advancements.
 *
 * <p>That mod does not extend the vanilla advancements screen, it REPLACES it:
 * its own mixin intercepts the screen swap and substitutes
 * {@link BetterAdvancementsScreen}. Vanilla's {@code AdvancementsScreen} is
 * then never shown, so our tab mixins against it apply to a class nobody
 * displays and the tab silently disappears. Nothing crashes and nothing
 * conflicts; the feature just stops existing. This restores it.
 *
 * <p><b>Positioning is delegated to their own code on purpose.</b> Their window
 * is screen-filling and user-scalable rather than vanilla's fixed 252x140, the
 * tab row is dynamically sized, and it paginates once the tabs overflow, so any
 * geometry we copied would be a snapshot that silently drifts. Instead this
 * asks {@link BetterAdvancementTabType} to draw, place and hit-test the tab at
 * our index, exactly as it does for their real tabs. If they change their
 * layout, ours moves with it.
 *
 * <p><b>Deliberately reduced scope:</b> the tab opens the standalone collection
 * screen instead of rendering the fish tree inside their window. The tree's
 * geometry is written against vanilla's fixed 234x113 interior; reproducing it
 * at their dynamic size is real work that cannot be verified without looking at
 * it, and shipping unverified UI is exactly how the 26.2 crash happened. The
 * feature is reachable and discoverable again, which was what their users lost.
 */
@Mixin(BetterAdvancementsScreen.class)
public abstract class BetterAdvancementsScreenMixin extends Screen {

    @Shadow
    @Final
    private Map<AdvancementHolder, BetterAdvancementTab> tabs;

    /** Their tab pagination cursor; the click hit box must honour it. */
    @Shadow
    private static int tabPage;

    @Shadow
    protected int internalWidth;

    @Shadow
    protected int internalHeight;

    /** Their window insets; private constants over there, restated here. */
    @Unique
    private static final int RFF_SIDE = 30;
    @Unique
    private static final int RFF_TOP = 40;

    protected BetterAdvancementsScreenMixin(Component title) {
        super(title);
    }

    /**
     * Our tab sits immediately after their real ones, so it only exists on the
     * page that index falls on. Returns -1 when it is not on the visible page,
     * or when the row cannot hold it at this window size.
     */
    @Unique
    private int rff$visibleIndex(int windowWidth, int windowHeight, int maxTabs, int skip) {
        int index = this.tabs.size();
        if (index - skip < 0 || index - skip >= maxTabs) {
            return -1; // another page
        }
        return BetterAdvancementTabType.getTabType(windowWidth, windowHeight, index) == null ? -1 : index;
    }

    @Inject(method = "renderWindow", at = @At("TAIL"))
    private void rff$drawFishTab(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom,
            int maxTabs, int skip, CallbackInfo ci) {
        int windowWidth = right - left;
        int windowHeight = bottom - top;
        int index = rff$visibleIndex(windowWidth, windowHeight, maxTabs, skip);
        if (index < 0) {
            return;
        }
        BetterAdvancementTabType type = BetterAdvancementTabType.getTabType(windowWidth, windowHeight, index);
        type.draw(graphics, left, top, windowWidth, windowHeight, false, index);
        type.drawIcon(graphics, left, top, windowWidth, windowHeight, index, FishTabView.icon());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rff$clickFishTab(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0 || event.modifiers() != 0) {
            return;
        }
        // Their own mouseClicked recomputes the window box from scratch rather
        // than reusing render state; mirror it so the hit box cannot drift from
        // where the tab was actually drawn.
        int left = RFF_SIDE + (this.width - this.internalWidth) / 2;
        int top = RFF_TOP + (this.height - this.internalHeight) / 2;
        int right = this.internalWidth - RFF_SIDE + (this.width - this.internalWidth) / 2;
        int bottom = this.internalHeight - RFF_SIDE + (this.height - this.internalHeight) / 2;
        int windowWidth = right - left;
        int windowHeight = bottom - top;

        int maxTabs = BetterAdvancementTabType.getMaxTabs(windowWidth, windowHeight);
        int index = rff$visibleIndex(windowWidth, windowHeight, maxTabs, tabPage * maxTabs);
        if (index < 0) {
            return;
        }
        BetterAdvancementTabType type = BetterAdvancementTabType.getTabType(windowWidth, windowHeight, index);
        if (type.isMouseOver(left, top, windowWidth, windowHeight, index, event.x(), event.y())) {
            // The single-argument constructor opens on the first pattern, the
            // same entry point the collection keybind uses. Passing an index
            // here would floorMod into an arbitrary pattern.
            this.minecraft.setScreenAndShow(new CollectionScreen((Screen) (Object) this));
            cir.setReturnValue(true);
        }
    }
}
