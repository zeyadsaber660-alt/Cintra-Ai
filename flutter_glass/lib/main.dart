import 'package:flutter/cupertino.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

/// Add-to-app entry point. Renders a real `liquid_glass_widgets` LiquidGlass
/// surface (custom fragment shader: blur + specular + chromatic aberration)
/// over a colourful backdrop so the refraction is visible. Embedded in the
/// Cintra host app via a FlutterActivity (see GlassFlutterActivity).
void main() async {
  await LiquidGlassWidgets.initialize();
  runApp(LiquidGlassWidgets.wrap(child: const GlassDemoApp()));
}

class GlassDemoApp extends StatelessWidget {
  const GlassDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      backgroundColor: const Color(0xFF0C0D10),
      child: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          Positioned(top: -50, left: -40, child: _blob(const Color(0xFF38BDF8), 280)),
          Positioned(bottom: -70, right: -30, child: _blob(const Color(0xFFD97706), 260)),
          Positioned(top: 120, right: 40, child: _blob(const Color(0xFF34D399), 180)),
          Center(
            child: GlassCard(
              width: 230,
              height: 128,
              child: const Center(
                child: Text(
                  'Cintra · Flutter LiquidGlass',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Color(0xFFF5F6F8),
                    fontSize: 16,
                    height: 1.35,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _blob(Color color, double size) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(
          colors: <Color>[color.withValues(alpha: 0.55), color.withValues(alpha: 0.0)],
        ),
      ),
    );
  }
}