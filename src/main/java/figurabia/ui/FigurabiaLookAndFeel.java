/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 05.09.2009
 */
package figurabia.ui;

import org.pushingpixels.substance.api.ColorSchemeTransform;
import org.pushingpixels.substance.api.DecorationAreaType;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.api.SubstanceColorSchemeBundle;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.SubstanceSkin;
import org.pushingpixels.substance.api.colorscheme.OrangeColorScheme;
import org.pushingpixels.substance.api.skin.BusinessBlackSteelSkin;

@SuppressWarnings("serial")
public class FigurabiaLookAndFeel extends SubstanceLookAndFeel {

    //private final static Color MID_COLOR = new Color(191, 153, 0);
    //private final static Color ULTRA_LIGHT_COLOR = new Color(255, 204, 0);

    private static SubstanceSkin createSkin() {
        SubstanceSkin skin = new BusinessBlackSteelSkin();
        ColorSchemeTransform transform = new ColorSchemeTransform() {
            @Override
            public SubstanceColorScheme transform(SubstanceColorScheme scheme) {
                return scheme.hueShift(0.48).saturate(-1.0).shade(0.1);
            }
        };
        skin = skin.transform(transform, "Business Black Steel, Orange Edition");
        SubstanceColorSchemeBundle bundle = new SubstanceColorSchemeBundle(
                new OrangeColorScheme().hueShift(0.023).saturate(0.5),
                skin.getEnabledColorScheme(DecorationAreaType.NONE),
                skin.getDisabledColorScheme(DecorationAreaType.NONE));
        skin.registerDecorationAreaSchemeBundle(bundle, DecorationAreaType.NONE);
        return skin;
    }

    public FigurabiaLookAndFeel() {
        super(createSkin());
    }
}
