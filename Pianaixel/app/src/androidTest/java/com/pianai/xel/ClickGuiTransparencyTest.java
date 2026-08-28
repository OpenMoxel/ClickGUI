/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */

package com.pianai.xel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies that the Canvas overlay leaves host pixels untouched outside ClickGUI surfaces. */
@RunWith(AndroidJUnit4.class)
public final class ClickGuiTransparencyTest {

    private static final int REFERENCE_WIDTH = 1920;
    private static final int REFERENCE_HEIGHT = 1080;

    @Test
    public void clickGuiLeavesExternalPixelsFullyTransparent() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Bitmap bitmap = Bitmap.createBitmap(
                REFERENCE_WIDTH,
                REFERENCE_HEIGHT,
                Bitmap.Config.ARGB_8888
        );

        try {
            instrumentation.runOnMainSync(() -> {
                ClickGuiView view = new ClickGuiView(
                        instrumentation.getTargetContext().getApplicationContext()
                );
                int widthSpec = View.MeasureSpec.makeMeasureSpec(
                        REFERENCE_WIDTH, View.MeasureSpec.EXACTLY
                );
                int heightSpec = View.MeasureSpec.makeMeasureSpec(
                        REFERENCE_HEIGHT, View.MeasureSpec.EXACTLY
                );
                view.measure(widthSpec, heightSpec);
                view.layout(0, 0, REFERENCE_WIDTH, REFERENCE_HEIGHT);
                view.draw(new Canvas(bitmap));
            });

            assertTransparent(bitmap, 0, 0);
            assertTransparent(bitmap, REFERENCE_WIDTH - 1, 0);
            // The intended bottom information bar starts at y=999, so y=998 remains an
            // external transparent corner on both sides.
            assertTransparent(bitmap, 0, 998);
            assertTransparent(bitmap, REFERENCE_WIDTH - 1, 998);
            assertTransparent(bitmap, 120, 120);
            assertTransparent(bitmap, 1740, 150);
            assertTransparent(bitmap, 120, 960);
            assertTransparent(bitmap, 1740, 780);

            int panelAlpha = Color.alpha(bitmap.getPixel(500, 200));
            assertTrue("main panel must remain translucent", panelAlpha > 0 && panelAlpha < 255);

            int rowAlpha = Color.alpha(bitmap.getPixel(1000, 340));
            assertTrue("module row must remain translucent", rowAlpha > 0 && rowAlpha < 255);

            int infoBarAlpha = Color.alpha(bitmap.getPixel(300, 1040));
            assertTrue("the intentional bottom information bar must remain translucent",
                    infoBarAlpha > 0 && infoBarAlpha < 255);
        } finally {
            bitmap.recycle();
        }
    }

    private static void assertTransparent(Bitmap bitmap, int x, int y) {
        assertEquals("expected transparent pixel at (" + x + ", " + y + ")",
                0, Color.alpha(bitmap.getPixel(x, y)));
    }
}
