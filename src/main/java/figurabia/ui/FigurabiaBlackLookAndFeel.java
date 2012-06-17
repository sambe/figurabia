/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 08.03.2010
 */
package figurabia.ui;

import org.pushingpixels.substance.api.ColorSchemeTransform;
import org.pushingpixels.substance.api.DecorationAreaType;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.api.SubstanceColorSchemeBundle;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.SubstanceSkin;
import org.pushingpixels.substance.api.colorscheme.OrangeColorScheme;
import org.pushingpixels.substance.api.skin.RavenSkin;

@SuppressWarnings("serial")
public class FigurabiaBlackLookAndFeel extends SubstanceLookAndFeel {

    private static SubstanceSkin createSkin() {
        SubstanceSkin skin = new RavenSkin();
        ColorSchemeTransform transform = new ColorSchemeTransform() {
            @Override
            public SubstanceColorScheme transform(SubstanceColorScheme scheme) {
                return scheme.saturate(-1.0);
            }
        };
        skin = skin.transform(transform, "Figurabia Black");
        SubstanceColorSchemeBundle bundle = new SubstanceColorSchemeBundle(
                new OrangeColorScheme().hueShift(0.023).saturate(0.85),
                skin.getEnabledColorScheme(DecorationAreaType.NONE),
                skin.getDisabledColorScheme(DecorationAreaType.NONE));
        skin.registerDecorationAreaSchemeBundle(bundle, DecorationAreaType.NONE);
        return skin;
    }

    public FigurabiaBlackLookAndFeel() {
        super(createSkin());
    }
}
