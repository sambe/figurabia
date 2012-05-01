/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 08.03.2010
 */
package figurabia.ui;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.api.ColorSchemeTransform;
import org.jvnet.substance.api.SubstanceColorScheme;
import org.jvnet.substance.api.SubstanceColorSchemeBundle;
import org.jvnet.substance.api.SubstanceSkin;
import org.jvnet.substance.colorscheme.OrangeColorScheme;
import org.jvnet.substance.painter.decoration.DecorationAreaType;
import org.jvnet.substance.skin.RavenSkin;

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
                new OrangeColorScheme().hueShift(0.023).saturate(0.85), skin.getMainDefaultColorScheme(),
                skin.getMainDisabledColorScheme());
        skin.registerDecorationAreaSchemeBundle(bundle, DecorationAreaType.NONE);
        return skin;
    }

    public FigurabiaBlackLookAndFeel() {
        super(createSkin());
    }
}
